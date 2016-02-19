/*
 * Copyright 2014-2016 Key Bridge LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ftdichip.usb;

import com.ftdichip.usb.enumerated.*;
import java.util.Arrays;
import java.util.Collection;
import javax.usb3.IUsbDevice;
import javax.usb3.UsbHostManager;
import javax.usb3.enumerated.EEndpointDirection;
import javax.usb3.exception.UsbException;
import javax.usb3.request.BMRequestType;

/**
 * Utility class to detect and configure FTDI UART chips via the USB bus.
 * <p>
 * This library is tuned to communicate and configure FT232R, FT2232 and FT232B
 * series chips from Future Technology Devices International Ltd.
 * <p>
 * Routines and BYTE value constant in this class are rewritten in Java from the
 * native {@code "libftdi"} library originally written in C.
 * <p>
 * This utility class includes methods useful for <em>identifying</em>,
 * <em>configuring</em> and <em>controlling</em> the USB port for a serial
 * <strong>port</strong>. To <em>read from</em> and <em>write to</em> a specific
 * USB <strong>device</strong> use the {@link FTDI} class.
 * <p>
 * This utility class allows a user program to identify all attached FTDI UART
 * chips on the USB via {@link #findFTDIDevices()}, then to communicate with
 * each using multiple instances of the {@link FTDI} class.
 *
 * @see <a
 * href="https://github.com/legege/libftdi/blob/master/src/ftdi.c">ftdi.c</a>
 * @see <a
 * href="https://github.com/legege/libftdi/blob/master/src/ftdi.h">ftdi.c</a>
 * @author Jesse Caulfield
 * @since v1.0.0 April, 25, 2014
 */
public class FTDIUtility {

  /**
   * Future Technology Devices International Ltd. USB vendor ID. [0403]
   */
  public static final short VENDOR_ID = 0x0403;
  /**
   * The FT232R, FT2232 and FT232B USB to serial converter chip sets.
   * <p>
   * [6001, 6010, 6011]
   */
  public static final Short[] PRODUCT_ID = new Short[]{0x6001, 0x6010, 0x6011};
  /**
   * 115200 bps. The default baud rate for most FTDI chips.
   */
  public static final int DEFAULT_BAUD_RATE = 115200;

  //<editor-fold defaultstate="collapsed" desc="Static FTDI Byte Constant Declarations">
  /**
   * Length of the modem status header, transmitted with every read.
   */
  public static final int MODEM_STATUS_HEADER_LENGTH = 2;

  /**
   * FTDI vendor-specific USB device control message to WRITE a configuration
   * parameter.
   * <p>
   * This is the {@code bmRequestType} bitmapped field that identifies the
   * characteristics of a specific request.
   */
  public static final byte FTDI_USB_CONFIGURATION_WRITE = new BMRequestType(EEndpointDirection.HOST_TO_DEVICE,
                                                                            BMRequestType.EType.VENDOR,
                                                                            BMRequestType.ERecipient.DEVICE).getByteCode();
  /**
   * FTDI vendor-specific USB device control message to READ a configuration
   * parameter.
   * <p>
   * This is the {@code bmRequestType} bitmapped field that identifies the
   * characteristics of a specific request.
   */
  public static final byte FTDI_USB_CONFIGURATION_READ = new BMRequestType(EEndpointDirection.DEVICE_TO_HOST,
                                                                           BMRequestType.EType.VENDOR,
                                                                           BMRequestType.ERecipient.DEVICE).getByteCode();
// control requests codes ----------------------------------------------------
  /**
   * Reset the port
   */
  public static final byte SIO_RESET_REQUEST = 0x00;
  /**
   * Set the modem control register. Definition for flow control.
   */
  public static final byte SIO_SET_MODEM_CTRL_REQUEST = 0x01;
  /**
   * Set flow control register. Definition for flow control.
   */
  public static final byte SIO_SET_FLOW_CTRL_REQUEST = 0x02;
  /**
   * Set baud rate. Definition for flow control.
   */
  public static final byte SIO_SET_BAUDRATE_REQUEST = 0x03;
  /**
   * Set the data characteristics of the port. Definition for flow control.
   */
  public static final byte SIO_SET_DATA_REQUEST = 0x04;
  public static final byte SIO_POLL_MODEM_STATUS_REQUEST = 0x05;
//  public static final byte SIO_SET_EVENT_CHAR_REQUEST = 0x06;
//  public static final byte SIO_SET_ERROR_CHAR_REQUEST = 0x07;

//  public static final byte SIO_SET_LATENCY_TIMER_REQUEST = 0x09;
//  public static final byte SIO_GET_LATENCY_TIMER_REQUEST = 0x0A;
//  public static final byte SIO_SET_BITMODE_REQUEST = 0x0B;
//  public static final byte SIO_READ_PINS_REQUEST = 0x0C;
//  public static final byte SIO_READ_EEPROM_REQUEST = (byte) 0x90;
//  public static final byte SIO_WRITE_EEPROM_REQUEST = (byte) 0x91;
//  public static final byte SIO_ERASE_EEPROM_REQUEST = (byte) 0x92;
//  public static final byte SIO_RESET_SIO = 0;
//  public static final byte SIO_RESET_PURGE_RX = 1;
//  public static final byte SIO_RESET_PURGE_TX = 2;
  // DTR and RTS lines
  public static final byte SIO_SET_DTR_MASK = 0x1;
  public static final byte SIO_SET_DTR_HIGH = (byte) (1 | (SIO_SET_DTR_MASK << 8));
  public static final byte SIO_SET_DTR_LOW = (byte) ((SIO_SET_DTR_MASK << 8));
  public static final byte SIO_SET_RTS_MASK = 0x2;
  public static final byte SIO_SET_RTS_HIGH = (byte) (2 | (SIO_SET_RTS_MASK << 8));
  public static final byte SIO_SET_RTS_LOW = (byte) ((SIO_SET_RTS_MASK << 8));
//</editor-fold>

  // FTDI USB Utility Methods
  /**
   * Search the USB device tree and return all detected FTDI devices.
   * <p>
   * This method scans the USB tree for any device having a FTDI vendor ID
   * (0403) and a recognized UART product ID (6001, 6010, 6011).
   *
   * @return a non-null (but possibly empty) list of FTDI devices attached to
   *         the USB
   * @throws UsbException if the USB port cannot be read
   */
  public static Collection<IUsbDevice> findFTDIDevices() throws UsbException {
    return UsbHostManager.getUsbDeviceList(VENDOR_ID, Arrays.asList(PRODUCT_ID));
  }

  /**
   * Rest the FTDI UART configuration. This resets the serial port to its
   * default state.
   *
   * @param usbDevice the USB Device to send the control message to
   * @throws UsbException if the control message cannot be set
   */
  public void reset(IUsbDevice usbDevice) throws UsbException {
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_RESET_REQUEST,
                                                       (short) 0,
                                                       (short) 0));
  }

  /**
   * Set the link data rate (bits per second).
   * <p>
   * The maximum Baud rate achievable with FTDI's current devices is 3M Baud.
   * <p>
   * The FT232R, FT2232 and FT232B chip sets support all standard baud rates and
   * non-standard baud rates from 300 Baud up to 3 Megabaud. The achievable baud
   * rates range is 183.1 baud to 3,000,000 baud.
   *
   * @param usbDevice         the USB Device to send the control message to
   * @param requestedBaudRate the requested baud rate (bits per second). e.g.
   *                          115200.
   * @throws UsbException if the baud rate cannot be set
   */
  public static void setBaudRate(IUsbDevice usbDevice, int requestedBaudRate) throws UsbException, UsbException, UsbException {
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_BAUDRATE_REQUEST,
                                                       calculateBaudRate(requestedBaudRate),
                                                       (short) 0));
  }

  /**
   * A general method to set the baud rate for most all FTDI UART chip types.
   * <p>
   * This method uses the more general/generic baud rate calculator. It it
   * functionally identical to
   * the{@link #setBaudRate(javax.usb.IUsbDevice, int)} method and is included
   * in this class only for reference and code completeness..
   *
   * @param usbDevice         the USB Device to send the control message to
   * @param requestedBaudRate the requested baud rate (bits per second). e.g.
   *                          115200.
   * @return the actual baud rate assigned to the port
   * @throws UsbException if the baud rate cannot be set
   */
  public static int setBaudRate_General(IUsbDevice usbDevice, int requestedBaudRate) throws UsbException {
    /**
     * Convert the requested baud rate into an device configuration parameter.
     * Return the actual configured baud rate.
     */
    short[] baudRateConverted = convertBaudrate(requestedBaudRate);
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_BAUDRATE_REQUEST,
                                                       baudRateConverted[2],
                                                       baudRateConverted[1]));
    return baudRateConverted[0];
  }

  /**
   * Set the DTR line.
   *
   * @param usbDevice the FTDI USB device
   * @param state     TRUE to set high, FALSE to set low.
   * @throws UsbException if the device command message fails to set
   */
  public static void setDTR(IUsbDevice usbDevice, boolean state) throws UsbException {
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_MODEM_CTRL_REQUEST,
                                                       state ? SIO_SET_DTR_HIGH : SIO_SET_DTR_LOW,
                                                       (short) 0));
  }

  /**
   * Set the RTS line.
   *
   * @param usbDevice the FTDI USB device
   * @param state     TRUE to set high, FALSE to set low.
   * @throws UsbException if the device command message fails to set
   */
  public static void setRTS(IUsbDevice usbDevice, boolean state) throws UsbException {
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_MODEM_CTRL_REQUEST,
                                                       state ? SIO_SET_RTS_HIGH : SIO_SET_RTS_LOW,
                                                       (short) 0));
  }

  /**
   * Set the DTR and RTS lines.
   *
   * @param usbDevice the FTDI USB device
   * @param dtrState  TRUE to set high, FALSE to set low.
   * @param rtsState  TRUE to set high, FALSE to set low.
   * @throws UsbException if the device command message fails to set
   */
  public static void setDTRRTS(IUsbDevice usbDevice, boolean dtrState, boolean rtsState) throws UsbException {
    short dtrValue = dtrState ? SIO_SET_DTR_HIGH : SIO_SET_DTR_LOW;
    short rtsValue = rtsState ? SIO_SET_RTS_HIGH : SIO_SET_RTS_LOW;
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_MODEM_CTRL_REQUEST,
                                                       (short) (dtrValue | rtsValue),
                                                       (short) 0));
  }

  /**
   * Set flow control for the FTDI chip. The FT245R, FT2232C (in FIFO mode) and
   * FT245BM chips use their own handshaking as an integral part of its design,
   * by proper use of the TXE# line. The FT232R, FT2232C (in UART mode) and
   * FT232BM chips can use RTS/CTS, DTR/DSR hardware or XOn/XOff software
   * handshaking. It is highly recommended that some form of handshaking be
   * used.
   *
   * @param usbDevice   the FTDI USB device
   * @param flowcontrol flow control to use.
   * @throws UsbException if the device command message fails to set
   */
  public static void setFlowControl(IUsbDevice usbDevice, EFlowControl flowcontrol) throws UsbException {
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_FLOW_CTRL_REQUEST,
                                                       flowcontrol.getBytecode(),
                                                       (short) 0));
  }

  /**
   * Set (RS232) line characteristics. The break type can only be set via
   * {@linkplain #setLineProperty} and defaults to "BREAK_OFF".
   *
   * @param usbDevice the FTDI USB device
   * @param bits      Number of bits
   * @param stopbits  Number of stop bits
   * @param parity    LineParity mode
   * @throws UsbException if the device command message fails to set
   */
  public static void setLineProperty(IUsbDevice usbDevice, ELineDatabits bits, ELineStopbits stopbits, ELineParity parity) throws UsbException {
    setLineProperty(usbDevice, bits, stopbits, parity, ELineBreak.BREAK_OFF);
  }

  /**
   * Set (RS232) line characteristics.
   * <p>
   * This is a rewrite of the C method {@code ftdi_set_line_property2(....)}.
   *
   * @param usbDevice the FTDI USB device
   * @param bits      Number of bits
   * @param stopbits  Number of stop bits
   * @param parity    LineParity mode
   * @param breaktype Break type (default is BREAK_OFF)
   * @throws UsbException if the device command message fails to set
   */
  public static void setLineProperty(IUsbDevice usbDevice, ELineDatabits bits, ELineStopbits stopbits, ELineParity parity, ELineBreak breaktype) throws UsbException {
    short value = (short) bits.getBits();
    switch (parity) {
      case NONE:
//        value = (short) (value | (0x00 << 8));
        break;
      case ODD:
        value = (short) (value | (0x01 << 8));
        break;
      case EVEN:
        value = (short) (value | (0x02 << 8));
        break;
      case MARK:
        value = (short) (value | (0x03 << 8));
        break;
      case SPACE:
        value = (short) (value | (0x04 << 8));
        break;
      default:
        throw new AssertionError(parity.name());
    }
    switch (stopbits) {
      case STOP_BIT_1:
//        value = (short) (value | (0x00 << 11));
        break;
      case STOP_BIT_15:
        value = (short) (value | (0x01 << 11));
        break;
      case STOP_BIT_2:
        value = (short) (value | (0x01 << 11));
        break;
      default:
        throw new AssertionError(stopbits.name());
    }
    switch (breaktype) {
      case BREAK_OFF:
//        value = (short) (value | (0x00 << 14));
        break;
      case BREAK_ON:
        value = (short) (value | (0x01 << 14));
        break;
      default:
        throw new AssertionError(breaktype.name());
    }
    usbDevice.syncSubmit(usbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                       SIO_SET_DATA_REQUEST,
                                                       value,
                                                       (short) 0));
  }

  /**
   * FTDI Baud Rate Calculation.
   * <p>
   * A Baud rate for the FT232R, FT2232 (UART mode) or FT232B is generated using
   * the chips internal 48MHz clock. This is input to Baud rate generator
   * circuitry where it is then divided by 16 and fed into a prescaler as a 3MHz
   * reference clock. This 3MHz reference clock is then divided down to provide
   * the required Baud rate for the device's on chip UART. The value of the Baud
   * rate divisor is an integer plus a sub-integer prescaler. The original
   * FT8U232AM only allowed 3 sub- integer prescalers - 0.125, 0.25 or 0.5. The
   * FT232R, FT2232 (UART mode) and FT232B support a further 4 additional
   * sub-integer prescalers - 0.375, 0.625, 0.75, and 0.875. Thus, allowed
   * values for the Baud rate divisor are:
   * <p>
   * Divisor = n + 0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875; where n is an
   * integer between 2 and 16384 (214).
   * <p>
   * Note: Divisor = 1 and Divisor = 0 are special cases. A divisor of 0 will
   * give 3 MBaud, and a divisor of 1 will give 2 MBaud. Sub-integer divisors
   * between 0 and 2 are not allowed. Therefore the value of the divisor needed
   * for a given Baud rate is found by dividing 3000000 by the required Baud
   * rate.
   * <p>
   * The exact Baud rate may not be achievable - however as long as the actual
   * Baud rate used is within +/-3% of the required Baud rate then the link
   * should function without errors. When a Baud rate is passed to the driver
   * where the exact divisor required is not achievable the closest possible
   * Baud rate divisor will be used as long as that divisor gives a Baud rate
   * which is within +/- 3% of the Baud rate originally set.
   * <p>
   * For example: A non-standard Baud rate of 490000 Baud is required.
   * <p>
   * Required divisor = 3000000 / 490000 = 6.122
   * <p>
   * The closest achievable divisor is 6.125, which gives a baud rate of
   * 489795.9, which is well within the allowed +/- 3% margin of error.
   * Therefore 490000 can be passed to the driver and the device will
   * communicate without errors.
   *
   * @see See AN232B-05_BaudRates.pdf, page 7.
   *
   * @param requestedBaudRate the (possibly non-standard) requested baud rate
   *                          (bits per second)
   * @return the nearest supported baud rate (bits per second)
   */
  private static short calculateBaudRate(int requestedBaudRate) {
    /**
     * Developer note: The maximum Baud rate achievable with FTDI's current
     * devices is 3M Baud. the Baud rate divisor must be calculated using the
     * following formula:
     * <p>
     * {@code Integer Divisor + Sub-Integer Divisor = 3000000/Baud Rate}
     * <p>
     * where the Integer Divisor is any integer between 2 and 16384 and the
     * Sub-Integer Divisor can be any one of 0, 0.125, 0.25, 0.375, 0.5, 0.625,
     * 0.75 or 0.875. Note that the FT8U232AM device will only support
     * Sub-Integer Divisors of 0, 0.125, 0.25 and 0.5.
     * <p>
     * This 3MHz reference clock is then divided down to provide the required
     * Baud rate for the device's on chip UART. The value of the Baud rate
     * divisor is an integer plus a sub-integer pre-scaler.
     * <p>
     * Allowed values for the Baud rate divisor are: Divisor = n + 0, 0.125,
     * 0.25, 0.375, 0.5, 0.625, 0.75, 0.875; where n is an integer between 2 and
     * 16384 (2^14). n > 2.
     * <p>
     * The value of the divisor needed for a given Baud rate is found by
     * dividing 3000000 (= 3 MHz) by the required Baud rate.
     */
    int divisor = 3000000 / requestedBaudRate;
    /**
     * The exact Baud rate may not be achievable - however as long as the actual
     * Baud rate used is within +/-3% of the required Baud rate then the link
     * should function without errors. When a Baud rate is passed to the driver
     * where the exact divisor required is not achievable the closest possible
     * Baud rate divisor will be used as long as that divisor gives a Baud rate
     * which is within +/- 3% of the Baud rate originally set.
     */
    return (short) ((divisor << 16) >> 16);
  }

  //<editor-fold defaultstate="collapsed" desc="Deprecated Native Translations">
  /**
   * Convert the requested baud rate into an device configuration parameter.
   * Return the actual configured baud rate.
   *
   * @param baudrate the requested baud rate (bits per second). e.g. 115200.
   * @return the FTDI baudrate configuration value (two bytes)
   */
  private static short[] convertBaudrate(int baudrate) {
    // TODO(mikey): Braindead transcription of libfti method.  Clean up,
    // using more idiomatic Java where possible.
    int divisor = 24000000 / baudrate;
    int bestDivisor = 0;
    Integer bestBaud = 0;
    int bestBaudDiff = 0;
    int fracCode[] = {
      0, 3, 2, 4, 1, 5, 6, 7
    };

    for (int i = 0; i < 2; i++) {
      int tryDivisor = divisor + i;
      int baudEstimate;
      int baudDiff;

      if (tryDivisor <= 8) {
        // Round up to minimum supported divisor
        tryDivisor = 8;
//            } else if (mType != DeviceType.TYPE_AM && tryDivisor < 12) {
//                // BM doesn't support divisors 9 through 11 inclusive
//                tryDivisor = 12;
//            } else if (divisor < 16) {
//                // AM doesn't support divisors 9 through 15 inclusive
//                tryDivisor = 16;
//            } else {
//                if (mType == DeviceType.TYPE_AM) {
//                    // TODO
//                } else {
//                    if (tryDivisor > 0x1FFFF) {
//                        // Round down to maximum supported divisor value (for
//                        // BM)
//                        tryDivisor = 0x1FFFF;
//                    }
//                }
      }

      // Get estimated baud rate (to nearest integer)
      baudEstimate = (24000000 + (tryDivisor / 2)) / tryDivisor;

      // Get absolute difference from requested baud rate
      if (baudEstimate < baudrate) {
        baudDiff = baudrate - baudEstimate;
      } else {
        baudDiff = baudEstimate - baudrate;
      }

      if (i == 0 || baudDiff < bestBaudDiff) {
        // Closest to requested baud rate so far
        bestDivisor = tryDivisor;
        bestBaud = baudEstimate;
        bestBaudDiff = baudDiff;
        if (baudDiff == 0) {
          // Spot on! No point trying
          break;
        }
      }
    }

    // Encode the best divisor value
    long encodedDivisor = (bestDivisor >> 3) | (fracCode[bestDivisor & 7] << 14);
    // Deal with special cases for encoded value
    if (encodedDivisor == 1) {
      encodedDivisor = 0; // 3000000 baud
    } else if (encodedDivisor == 0x4001) {
      encodedDivisor = 1; // 2000000 baud (BM only)
    }

    // Split into "value" and "index" values
    Long value = encodedDivisor & 0xFFFF;
    Long index;
//        if (mType == DeviceType.TYPE_2232C || mType == DeviceType.TYPE_2232H
//                || mType == DeviceType.TYPE_4232H) {
//            index = (encodedDivisor >> 8) & 0xffff;
//            index &= 0xFF00;
//            index |= 0 /* TODO mIndex */;
//        } else {
    index = (encodedDivisor >> 16) & 0xffff;
//        }

    // Return the nearest baud rate
    return new short[]{bestBaud.shortValue(), index.shortValue(), value.shortValue()};
  }

  /**
   * Sets the chip baud rate. Method copied directly from "ftdi.c"
   *
   * @param iUsbDevice the FTDI USB device
   * @param baudrate   baud rate to set
   * @throws Exception if the command fails to set
   *
   * @deprecated 04/28/14 this method does not set the correct baud rate divisor
   * value (it is always zero). This and the supporting private calculator
   * method were translated from C but the original included an EEPROM query is
   * not available from JAVA. Use
   * {@link #setBaudRate(javax.usb.IUsbDevice, int)} or
   * {@link #setBaudRate_General(javax.usb.IUsbDevice, int)} instead.
   */
  public static void ftdi_set_baudrate(IUsbDevice iUsbDevice, int baudrate) throws Exception {
    short value = 0;
    short index = 0;
    int actual_baudrate;
    /**
     * Developer note: bitbang availability/enablement is identified from an
     * EEPROM query.
     */
//    if (ftdi -> bitbang_enabled) {      baudrate = baudrate * 4;    }
    /**
     * Developer notes: ftdi_convert_baudrate updates the value but not the
     * index.
     */
    actual_baudrate = ftdi_convert_baudrate(baudrate, value, index);
    if (actual_baudrate <= 0) {
      throw new Exception("Bogus baudrate <= 0.");
    }
    /**
     * Check within tolerance (about 5%) and Catch overflows
     */
    if ((actual_baudrate * 2 < baudrate) || ((actual_baudrate < baudrate)
                                             ? (actual_baudrate * 21 < baudrate * 20)
                                             : (baudrate * 21 < actual_baudrate * 20))) {
      throw new Exception("Unsupported baudrate. Note: bitbang baudrates are automatically multiplied by 4");
    }
    //    device.syncSubmit(device.createUsbControlIrp(bmRequestType, SET_BAUDRATE_REQUEST, value, index));
    iUsbDevice.syncSubmit(iUsbDevice.createUsbControlIrp(FTDI_USB_CONFIGURATION_WRITE,
                                                         SIO_SET_BAUDRATE_REQUEST,
                                                         value,
                                                         index));
  }

  /**
   * ftdi_convert_baudrate returns nearest supported baud rate to that
   * requested. Function is only used internally \internal
   *
   * @param baudrate the baud rate to set
   * @param value    a short value
   * @param index    an index value
   * @return the baudrate value
   * @deprecated see ftdi_set_baudrate for details.
   */
  private static int ftdi_convert_baudrate(int baudrate, short value, short index) {
    int best_baud;
    long encoded_divisor = 0;

    if (baudrate <= 0) {
      // Return error
      return -1;
    }
    int H_CLK = 120000000; // 120 MHz clock
    int C_CLK = 48000000; // 48 MHz clock

    EChipType type = EChipType.TYPE_2232H;

    if (type.equals(EChipType.TYPE_2232H) || type.equals(EChipType.TYPE_4232H) || type.equals(EChipType.TYPE_232H)) {
      if (baudrate * 10 > H_CLK / 0x3fff) {
        /*
         * On H Devices, use 12 000 000 Baudrate when possible. We have a 14 bit
         * divisor, a 1 bit divisor switch (10 or 16) three fractional bits and
         * a 120 MHz clock Assume AN_120 "Sub-integer divisors between 0 and 2
         * are not allowed" holds for DIV/10 CLK too, so /1, /1.5 and /2 can be
         * handled the same
         */
        best_baud = ftdi_to_clkbits(baudrate, H_CLK, 10, encoded_divisor);
        encoded_divisor |= 0x20000;
        /**
         * switch on CLK/10
         */
      } else {
        best_baud = ftdi_to_clkbits(baudrate, C_CLK, 16, encoded_divisor);
      }
    } else if (type.equals(EChipType.TYPE_BM) || type.equals(EChipType.TYPE_2232C) || type.equals(EChipType.TYPE_R)) {
      best_baud = ftdi_to_clkbits(baudrate, C_CLK, 16, encoded_divisor);
    } else {
      best_baud = ftdi_to_clkbits_AM(baudrate, encoded_divisor);
    }
    // Split into "value" and "index" values
    value = (short) (encoded_divisor & 0xFFFF);
    System.out.println("debug ftdi_convert_baudrate encoded_divisor " + encoded_divisor + " value " + value);
    if (type.equals(EChipType.TYPE_2232H) || type.equals(EChipType.TYPE_4232H) || type.equals(EChipType.TYPE_232H)) {
      index = (short) (encoded_divisor >> 8);
      index &= 0xFF00;
//      index |= ftdi -> index;
    } else {
      index = (short) (encoded_divisor >> 16);
    }
    // Return the nearest baud rate
    System.out.println("debug ftdi_convert_baudrate best_baud " + best_baud + " value " + value + " index " + index);
    return best_baud;
  }

  /**
   * ftdi_to_clkbits Convert a requested baudrate for a given system clock and
   * predivisor to encoded divisor and the achievable baudrate Function is only
   * used internally \internal
   * <p>
   * See AN120 clk/1 -> 0 clk/1.5 -> 1 clk/2 -> 2 From /2, 0.125 steps may be
   * taken. The fractional part has frac_code encoding
   * <p>
   * value[13:0] of value is the divisor index[9] mean 12 MHz Base(120 MHz/10)
   * rate versus 3 MHz (48 MHz/16) else
   * <p>
   * H Type have all features above with {index[8],value[15:14]} is the encoded
   * subdivisor
   * <p>
   * FT232R, FT2232 and FT232BM have no option for 12 MHz and with
   * {index[0],value[15:14]} is the encoded subdivisor
   * <p>
   * AM Type chips have only four fractional subdivisors at value[15:14] for
   * subdivisors 0, 0.5, 0.25, 0.125
   *
   * @param baudrate        the baudrate
   * @param clk             a clock rate
   * @param clk_div         a clock divisor
   * @param encoded_divisor the encoded divisor
   * @return a clock bit
   * @deprecated see ftdi_set_baudrate for details.
   */
  private static int ftdi_to_clkbits(int baudrate, int clk, int clk_div, long encoded_divisor) {
    byte frac_code[] = {0, 3, 2, 4, 1, 5, 6, 7};
    int best_baud, divisor, best_divisor;
    if (baudrate >= clk / clk_div) {
      encoded_divisor = 0;
      best_baud = clk / clk_div;
    } else if (baudrate >= clk / (clk_div + clk_div / 2)) {
      encoded_divisor = 1;
      best_baud = clk / (clk_div + clk_div / 2);
    } else if (baudrate >= clk / (2 * clk_div)) {
      encoded_divisor = 2;
      best_baud = clk / (2 * clk_div);
    } else {
      /*
       * We divide by 16 to have 3 fractional bits and one bit for rounding
       */
      divisor = clk * 16 / clk_div / baudrate;
      /**
       * Decide if to round up or down
       */
      if ((divisor & 1) == 1) {
        best_divisor = divisor / 2 + 1;
      } else {
        best_divisor = divisor / 2;
      }
      if (best_divisor > 0x20000) {
        best_divisor = 0x1ffff;
      }
      best_baud = clk * 16 / clk_div / best_divisor;
      /**
       * Decide if to round up or down
       */
      if ((best_baud & 1) == 1) {
        best_baud = best_baud / 2 + 1;
      } else {
        best_baud /= 2;
      }
      encoded_divisor = (best_divisor >> 3) | (frac_code[best_divisor & 0x7] << 14);
    }
    return best_baud;
  }

  /**
   * ftdi_to_clkbits_AM For the AM device, convert a requested baudrate to
   * encoded divisor and the achievable baudrate Function is only used
   * internally \internal
   * <p>
   * See AN120 clk/1 -> 0 clk/1.5 -> 1 clk/2 -> 2 From /2, 0.125/ 0.25 and 0.5
   * steps may be taken The fractional part has frac_code encoding
   *
   * @param baudrate        the baudrate
   * @param encoded_divisor the encoded divisor
   * @return a int value
   * @deprecated see ftdi_set_baudrate for details.
   */
  @SuppressWarnings({"AssignmentToMethodParameter", "UnusedAssignment"})
  private static int ftdi_to_clkbits_AM(int baudrate, long encoded_divisor) {
    byte frac_code[] = {0, 3, 2, 4, 1, 5, 6, 7};
    byte am_adjust_up[] = {0, 0, 0, 1, 0, 3, 2, 1};
    byte am_adjust_dn[] = {0, 0, 0, 1, 0, 1, 2, 3};
    int best_divisor, best_baud, best_baud_diff;
    int divisor = 24000000 / baudrate;
    int i;

    // Round down to supported fraction (AM only)
    divisor -= am_adjust_dn[divisor & 7];

    // Try this divisor and the one above it (because division rounds down)
    best_divisor = 0;
    best_baud = 0;
    best_baud_diff = 0;
    for (i = 0; i < 2; i++) {
      int try_divisor = divisor + i;
      int baud_estimate;
      int baud_diff;

      // Round up to supported divisor value
      if (try_divisor <= 8) {
        // Round up to minimum supported divisor
        try_divisor = 8;
      } else if (divisor < 16) {
        // AM doesn't support divisors 9 through 15 inclusive
        try_divisor = 16;
      } else {
        // Round up to supported fraction (AM only)
        try_divisor += am_adjust_up[try_divisor & 7];
        if (try_divisor > 0x1FFF8) {
          // Round down to maximum supported divisor value (for AM)
          try_divisor = 0x1FFF8;
        }
      }
      // Get estimated baud rate (to nearest integer)
      baud_estimate = (24000000 + (try_divisor / 2)) / try_divisor;
      // Get absolute difference from requested baud rate
      if (baud_estimate < baudrate) {
        baud_diff = baudrate - baud_estimate;
      } else {
        baud_diff = baud_estimate - baudrate;
      }
      if (i == 0 || baud_diff < best_baud_diff) {
        // Closest to requested baud rate so far
        best_divisor = try_divisor;
        best_baud = baud_estimate;
        best_baud_diff = baud_diff;
        if (baud_diff == 0) {
          // Spot on! No point trying
          break;
        }
      }
    }
    // Encode the best divisor value
    encoded_divisor = (best_divisor >> 3) | (frac_code[best_divisor & 7] << 14);
    // Deal with special cases for encoded value
    if (encoded_divisor == 1) {
      encoded_divisor = 0;    // 3000000 baud
    } else if (encoded_divisor == 0x4001) {
      encoded_divisor = 1;    // 2000000 baud (BM only)
    }
    return best_baud;
  }//</editor-fold>

}
