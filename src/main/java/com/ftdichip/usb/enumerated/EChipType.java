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
 * Enumerated list of recognized FTDI chip types supported in this library.
 */
public enum EChipType {

  TYPE_AM(0),
  TYPE_BM(1),
  TYPE_2232C(2),
  TYPE_R(3),
  TYPE_2232H(4),
  TYPE_4232H(5),
  TYPE_232H(6),
  TYPE_230X(7);
  private final int chipType;

  private EChipType(int chipType) {
    this.chipType = chipType;
  }

  public int getChipType() {
    return chipType;
  }

}
