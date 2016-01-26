/*
 * Copyright (C) 2014 Jesse Caulfield <jesse@caulfield.org>.
 *
 * This library is free software), you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation), either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY), without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library), if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.ftdichip.usb.enumerated;

/**
 * Flow Control
 * <p>
 * The FT245R, FT2232C (in FIFO mode) and FT245BM chips use their own
 * handshaking as an integral part of its design, by proper use of the TXE#
 * line. The FT232R, FT2232C (in UART mode) and FT232BM chips can use RTS/CTS,
 * DTR/DSR hardware or XOn/XOff software handshaking. It is highly recommended
 * that some form of handshaking be used.
 * <p>
 * It is strongly encouraged that flow control is used because it is impossible
 * to ensure that the FTDI driver will always be scheduled. The chip can buffer
 * up to 384 bytes of data. Windows can 'starve' the driver program of time if
 * it is doing other things. The most obvious example of this is moving an
 * application around the screen with the mouse by grabbing its task bar. This
 * will result in a lot of graphics activity and data loss will occur if
 * receiving data at 115200 baud (as an example) with no handshaking.
 * <p>
 * If the data rate is low or data loss is acceptable then flow control may be
 * omitted.
 * <p>
 * @author Jesse Caulfield
 */
public enum EFlowControl {

  /**
   * None - this may result in data loss at high speeds.
   */
  DISABLE_FLOW_CTRL((byte) 0x0),
  /**
   * RTS/CTS - 2 wire handshake. The device will transmit if CTS is active and
   * will drop RTS if it cannot receive any more.
   */
  RTS_CTS_HS((byte) (0x1 << 8)),
  /**
   * DTR/DSR - 2 wire handshake. The device will transmit if DSR is active and
   * will drop DTR if it cannot receive any more.
   */
  DTR_DSR_HS((byte) (0x2 << 8)),
  /**
   * XON/XOFF - flow control is done by sending or receiving special characters.
   * One is XOn (transmit on) the other is XOff (transmit off). They are
   * individually programmable to any value.
   */
  XON_XOFF_HS((byte) (0x4 << 8));
  private final byte bytecode;

  private EFlowControl(byte bytecode) {
    this.bytecode = bytecode;
  }

  public byte getBytecode() {
    return bytecode;
  }
}
