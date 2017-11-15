/*
 * Copyright 2016 Sai Pullabhotla.
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

package com.jmethods.catatumbo.entities;

import com.jmethods.catatumbo.Embedded;
import com.jmethods.catatumbo.Entity;
import com.jmethods.catatumbo.Identifier;
import com.jmethods.catatumbo.PropertyMapper;
import com.jmethods.catatumbo.custommappers.CurrencyMapper;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Aurelien Thieriot
 *
 */
@Entity
public class GenericEntity<T, Z> {

  @Identifier
  private long id;

  @Embedded
  private T embeddedGeneric;

  private Z generic;

  /**
   * @return the id
   */
  public long getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(long id) {
    this.id = id;
  }

  public T getEmbeddedGeneric() {
    return embeddedGeneric;
  }

  public void setEmbeddedGeneric(T embeddedGeneric) {
    this.embeddedGeneric = embeddedGeneric;
  }

  public Z getGeneric() {
    return generic;
  }

  public void setGeneric(Z generic) {
    this.generic = generic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GenericEntity<?, ?> that = (GenericEntity<?, ?>) o;
    return id == that.id &&
            Objects.equals(embeddedGeneric, that.embeddedGeneric) &&
            Objects.equals(generic, that.generic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, embeddedGeneric, generic);
  }

  public static GenericEntity<Address, String> createSampleGenericEntity1() {
    GenericEntity<Address, String> genericEntity = new GenericEntity<>();
    genericEntity.setEmbeddedGeneric(Address.getSample1());
    genericEntity.setGeneric("Stuff");
    return genericEntity;
  }
}
