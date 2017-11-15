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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Aurelien Thieriot
 * 
 */
public class GenericParameterizedType implements ParameterizedType {

  private Type[] types;

  public GenericParameterizedType(Type[] types) {
    this.types = types;
  }

  @Override
  public Type[] getActualTypeArguments() {
    return types;
  }

  @Override
  public Type getRawType() {
    return GenericEntity.class;
  }

  @Override
  public Type getOwnerType() {
    return null;
  }
}
