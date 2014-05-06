/*
 * Copyright (c) 2014, Jesse Caulfield <jesse@caulfield.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.ftdichip.usb;

import static com.ftdichip.usb.FTDIUtil.*;
import com.ftdichip.usb.enumerated.EFlowControl;
import com.ftdichip.usb.enumerated.ELineDatabits;
import com.ftdichip.usb.enumerated.ELineParity;
import com.ftdichip.usb.enumerated.ELineStopbits;
import java.util.Arrays;
import javax.usb.*;
import javax.usb.exception.UsbDisconnectedException;
import javax.usb.exception.UsbException;
import javax.usb.exception.UsbNotActiveException;
import javax.usb.ri.enumerated.EEndpointDirection;

/**
 * FTDI UART read/write utility. This class
 * <p>
 * This library is tuned to communicate with FT232R, FT2232 and FT232B chips
 * from Future Technology Devices International Ltd.
 * <p>
 * Developer note: There is an synchronous and asynchronous WRITE method but no
 * asynchronous READ method. This class wraps the USB I/O read transaction to
 * post-process returned USB packets and to strip FTDI-specific header
 * information before passing data to the reader. This strategy provides a clean
 * READ transaction but would require adding (yet another layer of) event
 * listener/notifier logic to support asynchronous reads. If you really need
 * ASYNC read then you should directly access the {@linkplain UsbPipe}, which
 * already has a listener interface.
 * <p>
 * @author Jesse Caulfield <jesse@caulfield.org> May 06, 2014
 */
public class FTDI {

  /**
   * 115200 bps. The default baud rate for most FTDI chips.
   */
  public static final int DEFAULT_BAUD_RATE = 115200;

  /**
   * The USB Device to which this FTDI instance is attached.
   */
  private final IUsbDevice usbDevice;
  /**
   * The USB interface (within the IUsbDevice) through which this device
   * communicates. This is extracted from the IUsbDevice and stored here (at the
   * class level) for convenience.
   * <p>
   * IUsbInterface a synchronous wrapper through which this application sends
   * and receives messages with the device.
   */
  private IUsbInterface usbInterface;
  /**
   * The USB Pipe used to READ data from the connected device.
   */
  private IUsbPipe usbPipeRead;
  /**
   * The USB Pipe used to WRITE data from the connected device.
   */
  private IUsbPipe usbPipeWrite;

  /**
   * Construct a new FTDI (read, write) instance.
   * <p>
   * The serial port is automatically configured for default operation at 115200
   * bps, 8 data bits, no parity, 1 stop bit, no flow control. Use
   * {@link #setSerialPort(int, com.ftdichip.usb.enumerated.ELineDatabits, com.ftdichip.usb.enumerated.ELineStopbits, com.ftdichip.usb.enumerated.ELineParity, com.ftdichip.usb.enumerated.EFlowControl)}
   * to set your required line configuration.
   * <p>
   * A user can identify all attached FTDI UART chips on the USB with methods in
   * the {@link #FTDIUtil} class, then communicate with each desired device
   * using one or more instances of this FTDI class.
   * <p>
   * @param usbDevice the specific UsbDevice instance to communicate with
   * @throws UsbException if the USB device is not readable/writable by the
   *                      current user (permission error)
   */
  public FTDI(IUsbDevice usbDevice) throws UsbException {
    /**
     * Set the USB Device.
     */
    this.usbDevice = usbDevice;
    /**
     * USB Interfaces: When you want to communicate with an interface or with
     * endpoints of this interface then you have to claim it before using it and
     * you have to release it when you are finished. Example:
     */
    IUsbConfiguration configuration = usbDevice.getActiveUsbConfiguration();
    /**
     * Developer note: FTDI devices have only ONE IUsbInterface (Interface #0).
     * Therefore always get and use the first available IUsbInterface from the
     * list.
     * <p>
     * The returned interface setting will be the current active alternate
     * setting if this configuration (and thus the contained interface) is
     * active. If this configuration is not active, the returned interface
     * setting will be an implementation-dependent alternate setting.
     */
    usbInterface = configuration.getUsbInterfaces().get(0);
    /**
     * Claim this USB interface. This will attempt whatever claiming the native
     * implementation provides, if any. If the interface is already claimed, or
     * the native claim fails, this will fail. This must be done before opening
     * and/or using any IUsbPipes.
     * <p>
     * Developer note: It is possible (nee likely) that the interface is already
     * used by the ftdi_sio kernel driver and mapped to a TTY device file.
     * Always force the claim by passing an interface policy to the claim
     * method:
     */
    usbInterface.claim(new IUsbInterfacePolicy() {

      @Override
      public boolean forceClaim(IUsbInterface usbInterface) {
        return true;
      }
    });
    /**
     * Scan the interface UsbEndPoint list to set the READ and WRITE USB pipes.
     */
    for (IUsbEndpoint usbEndpoint : usbInterface.getUsbEndpoints()) {
      if (EEndpointDirection.HOST_TO_DEVICE.equals(usbEndpoint.getDirection())) {
        usbPipeWrite = usbEndpoint.getUsbPipe();
      } else {
        usbPipeRead = usbEndpoint.getUsbPipe();
      }
    }
    /**
     * Set the serial line configuration.
     */
    FTDIUtil.setSerialPort(usbDevice, DEFAULT_BAUD_RATE, ELineDatabits.BITS_8, ELineStopbits.STOP_BIT_1, ELineParity.NONE, EFlowControl.DISABLE_FLOW_CTRL);
    /**
     * Add a shutdown hook to disconnect the USB interface and close the USB
     * port when shutting down. This will un-claim the device.
     */
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        try {
          usbInterface.release();
          Thread.sleep(250); // wait a quarter second for stuff to settle
        } catch (UsbException | UsbNotActiveException | UsbDisconnectedException | InterruptedException ex) {
        }
      }
    });
  }

  /**
   * Set the serial port configuration. This is a convenience method to send
   * multiple USB control messages to the FTDI device. It is a shortcut to the
   * same method in {@linkplain FTDIUtil}.
   * <p>
   * @param requestedBaudRate the requested baud rate (bits per second). e.g.
   *                          115200.
   * @param bits              Number of bits
   * @param stopbits          Number of stop bits
   * @param parity            LineParity mode
   * @param flowControl       flow control to use.
   * @throws UsbException if the FTDI UART cannot be configured or control
   *                      messages cannot be sent (e.g. insufficient
   *                      permissions)
   */
  public void setSerialPort(int requestedBaudRate,
                            ELineDatabits bits,
                            ELineStopbits stopbits,
                            ELineParity parity,
                            EFlowControl flowControl) throws UsbException {
    FTDIUtil.setSerialPort(usbDevice, requestedBaudRate, bits, stopbits, parity, flowControl);
  }

  /**
   * Asynchronously write a byte[] array to the FTDI port.
   * <p>
   * The default timeout value set in the properties file is used.
   * <p>
   * @param data A byte array containing the data to write to the device.
   * @exception UsbException If an error occurs.
   */
  public void writeAsync(byte[] data) throws UsbException {
    if (!usbPipeWrite.isOpen()) {
      usbPipeWrite.open();
    }
    IUsbIrp usbIrp = usbPipeWrite.createUsbIrp();
    usbIrp.setData(data);
    usbPipeWrite.asyncSubmit(usbIrp);
  }

  /**
   * Synchronously write a byte[] array to the FTDI port.
   * <p>
   * Developer note: This method returns immediately upon completion, which is
   * when the data has been stuffed into the device IN buffer. Some (slower)
   * devices require a little time to read out and process instructions in the
   * buffer. This is especially important during rapid-fire write/read
   * transactions (e.g. within a FOR/WHILE loop). If you need to add a
   * Thread.sleep() delay sleep values of between 5 and 10 milliseconds are
   * typically sufficient.
   * <p>
   * @param data A byte array containing the data to write to the device.
   * @return The number of bytes actually transferred to the device.
   * @exception UsbException If an error occurs.
   */
  public int write(byte[] data) throws UsbException {
    if (!usbPipeWrite.isOpen()) {
      usbPipeWrite.open();
    }
    return usbPipeWrite.syncSubmit(data);
  }

  /**
   * Synchronously read available data from the FTDI port and return the data as
   * a new byte[] array.
   * <p>
   * This method automatically strips the FTDI modem status header bytes and
   * only returns actual device data.
   * <p>
   * The maximum length of the array is the <code>wMaxPacketSize</code> value
   * provided by the USB EndPoint descriptor (typically 64 bytes but up to 512
   * bytes for some newer, faster chips). If there is no data on the device an
   * empty array (length = 0) is returned.
   * <p>
   * The actual length of the data array returned can be highly dependent upon
   * the query speed: e.g. slower HOST polling results in longer data arrays as
   * the device is able to stuff more data into its out buffer between each
   * request.
   * <p>
   * @return a non-null, variable length byte array containing the actual data
   *         read from the device. The actual length of the byte array ranges
   *         between 0 (empty) and 64 bytes (typ), but may be up to 512 bytes.
   * @throws UsbException if the USB Port fails to read
   */
  public byte[] read() throws UsbException {
    if (!usbPipeRead.isOpen()) {
      usbPipeRead.open();
    }
    byte[] usbPacket = new byte[usbPipeRead.getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()];
    int bytesRead = usbPipeRead.syncSubmit(usbPacket);
    /**
     * Return an empty array if there is no data on the line. Otherwise strip
     * the MODEM_STATUS_HEADER and return only the data.
     */
    return bytesRead == MODEM_STATUS_HEADER_LENGTH
      ? new byte[0]
      : Arrays.copyOfRange(usbPacket, MODEM_STATUS_HEADER_LENGTH, bytesRead);
  }
}
