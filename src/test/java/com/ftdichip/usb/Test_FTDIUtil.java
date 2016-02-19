/*
 * Copyright (C) 2014 Jesse Caulfield .
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.ftdichip.usb;

import java.util.Collection;
import javax.usb.IUsbDevice;
import javax.usb.exception.UsbException;
import javax.usb.utility.ByteUtility;
import org.junit.Test;

/**
 *
 * @author Jesse Caulfield
 */
public class Test_FTDIUtil {

  @Test
  public void testFTDI() throws UsbException {
    Collection<IUsbDevice> devices = FTDIUtility.findFTDIDevices();
    for (IUsbDevice iUsbDevice : devices) {
      System.out.println("FOUND FTDI device:  " + iUsbDevice);
    }

    IUsbDevice usbDevice = devices.iterator().next();
    FTDI ftdiDevice = FTDI.getInstance(usbDevice);
    // Read data from the FTDI device output buffer
    System.out.println("Read any data from the FTDI device output buffer");
    byte[] usbFrame = ftdiDevice.read();
    while (usbFrame.length > 0) {
      System.out.println("   READ " + usbFrame.length + " bytes: " + ByteUtility.toString(usbFrame));
      usbFrame = ftdiDevice.read();
    }

  }
}
