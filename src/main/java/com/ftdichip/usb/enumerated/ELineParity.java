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
 * LineParity mode for ftdi_set_line_property()
 */
public enum ELineParity {

  /**
   * No line parity.
   */
  NONE(0),
  /**
   * The parity bit is set to one if the number of ones in a given set of bits
   * is even (making the total number of ones, including the parity bit, odd).
   */
  ODD(1),
  /**
   * The parity bit is set to one if the number of ones in a given set of bits
   * is odd (making the total number of ones, including the parity bit, even).
   */
  EVEN(2),
  /**
   * The parity bit is always 1.
   */
  MARK(3),
  /**
   * The parity bit is always 0.
   */
  SPACE(4);
  private final int parity;

  private ELineParity(int parity) {
    this.parity = parity;
  }

  public int getParity() {
    return parity;
  }

}
