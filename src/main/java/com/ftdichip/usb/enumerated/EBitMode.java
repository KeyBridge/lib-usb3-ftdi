/*
 * Copyright (C) 2014 Jesse Caulfield <jesse@caulfield.org>.
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
package com.ftdichip.usb.enumerated;

/**
 * Enumerated list of Multi-Protocol Synchronous Serial Engine (MPSSE) bitbang
 * modes.
 * <p>
 * BIT BANG MODE: FT232R and FT245R chips can be set up in a special mode where
 * the normal function of the chips are replaced. This mode changes the 8 data
 * lines on the FT245BM or the RS232 data and control lines of the FT232BM to an
 * 8 bit bi-directional bus. The purpose of this mode was intended to be used to
 * program FPGA devices. It may also be used to talk to serial EEPPROMs or load
 * a data latch.
 * <p>
 * For the data to change, there has to be new data written and the baud clock
 * has to tick. If you do not write any new data to the device then the pins
 * will hold the last value written.
 * <p>
 * The commands of interest are :
 * <p>
 * 1) FT_SetBaudRate(ftHandle : Dword ; BaudRate : Dword) : FT_Result; This
 * controls the rate of transferring bytes that have been written to the device
 * onto the pins. It also sets the sampling of the pins and transferring the
 * result back to the Read path of the chip. The maximum baud rate is 3
 * MegaBaud. The clock for the Bit Bang mode is actually 16 times the baudrate.
 * A value of 9600 baud would transfer the data at (9600 x 16) = 153600 bytes
 * per second or 1 every 6.5 uS.
 * <p>
 * 2) FT_SetBitMode (ftHandle : Dword ; ucMask , ucEnable : Byte) : FT_Result;
 * This sets up which bits are input and which are output. The ucMask byte sets
 * the direction. A ‘1’ means the corresponding bit is to be an output. A ‘0’
 * means the corresponding bit is to be an input. When read data is passed back
 * to the PC, the current pin state for both inputs and outputs will used. The
 * ucEnable byte will turn the bit bang mode off and on. Setting bit 0 to ‘1’
 * turns it on. Clearing bit 0 to ‘0’ turns it off.
 * <p>
 * 3) FT_GetBitMode ( ftHandle : Dword ; pucData : pointer ) : FT_Result; This
 * function does an immediate read of the 8 pins and passes back the value. This
 * is useful to see what the pins are doing now. The normal Read pipe will
 * contain the same result but it has also been sampling the pins continuously
 * (up until its buffers become full). Therefor the data in the Read pipe will
 * be old.
 * <p>
 * @see Application Note AN232BM-01
 * @author Jesse Caulfield
 */
public enum EBitMode {

  /**
   * Switch off bitbang mode, back to regular serial/FIFO.
   */
  RESET((byte) 0x00),
  /**
   * Classical asynchronous bitbang mode, introduced with B-type chips.
   */
  BITBANG((byte) 0x01),
  /**
   * Multi-Protocol Synchronous Serial Engine, available on 2232x chips.
   */
  MPSSE((byte) 0x02),
  /**
   * Synchronous Bit-Bang Mode, available on 2232x and R-type chips.
   */
  SYNCBB((byte) 0x04),
  /**
   * MCU Host Bus Emulation Mode, available on 2232x chips. CPU-style fifo mode
   * gets set via EEPROM.
   */
  MCU((byte) 0x08),
  // CPU-style fifo mode gets set via EEPROM

  /**
   * Fast Opto-Isolated Serial Interface Mode, available on 2232x chips.
   */
  OPTO((byte) 0x10),
  /**
   * Bit-Bang on CBus pins of R-type chips, configure in EEPROM before use.
   */
  CBUS((byte) 0x20),
  /**
   * Single Channel Synchronous FIFO Mode, available on 2232H chips.
   */
  SYNCFF((byte) 0x40),
  /**
   * FT1284 mode, available on 232H chips
   */
  FT1284((byte) 0x80);

  private final byte mask;

  private EBitMode(byte mask) {
    this.mask = mask;
  }

}
