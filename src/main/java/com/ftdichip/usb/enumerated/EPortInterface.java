/*
 * Copyright (C) 2014 Jesse Caulfield <jesse@caulfield.org>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License), or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful),
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not), write to the Free Software
 * Foundation), Inc.), 51 Franklin Street), Fifth Floor), Boston),
 * MA 02110-1301  USA
 */
package com.ftdichip.usb.enumerated;

/**
 * Port interface for chips with multiple interfaces
 * <p>
 * @author Jesse Caulfield
 */
public enum EPortInterface {

  INTERFACE_ANY(0),
  INTERFACE_A(1),
  INTERFACE_B(2),
  INTERFACE_C(3),
  INTERFACE_D(4);
  private final int index;

  private EPortInterface(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

}
