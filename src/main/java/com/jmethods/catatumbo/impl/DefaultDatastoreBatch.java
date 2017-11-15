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

package com.jmethods.catatumbo.impl;

import java.lang.reflect.Type;
import java.util.List;

import com.google.cloud.datastore.Batch;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.FullEntity;
import com.jmethods.catatumbo.DatastoreBatch;
import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.impl.Marshaller.Intent;

/**
 * Default implementation of {@link DatastoreBatch} to execute batch updates.
 * 
 * @author Sai Pullabhotla
 *
 */
public class DefaultDatastoreBatch implements DatastoreBatch {

  /**
   * Reference to the entity manager
   */
  private DefaultEntityManager entityManager;

  /**
   * Native Batch object
   */
  private Batch nativeBatch = null;

  /**
   * A reference to the Writer for performing the updates.
   */
  private DefaultDatastoreWriter writer = null;

  /**
   * A reference to the Datastore
   */
  private Datastore datastore = null;

  /**
   * Creates a new instance of <code>DefaultDatastoreBatch</code>.
   * 
   * @param entityManager
   *          a reference to the entity manager
   */
  public DefaultDatastoreBatch(DefaultEntityManager entityManager) {
    this.entityManager = entityManager;
    this.datastore = entityManager.getDatastore();
    this.nativeBatch = datastore.newBatch();
    this.writer = new DefaultDatastoreWriter(this);
  }

  /**
   * Returns the entity manager from which this batch was created.
   * 
   * @return the entity manager from which this batch was created.
   */
  public DefaultEntityManager getEntityManager() {
    return entityManager;
  }

  /**
   * Returns the native batch.
   * 
   * @return the native batch
   */
  public Batch getNativeBatch() {
    return nativeBatch;
  }

  @Override
  public <E> E insert(E entity) {
    return insert(entity, null);
  }

  @Override
  public <E> E insert(E entity, Type entityType) {
    return writer.insert(entity, entityType);
  }

  @Override
  public <E> List<E> insert(List<E> entities) {
    return insert(entities, null);
  }

  @Override
  public <E> List<E> insert(List<E> entities, Type entityType) {
    return writer.insert(entities, entityType);
  }

  @Override
  public <E> void insertWithDeferredIdAllocation(E entity) {
    insertWithDeferredIdAllocation(entity, null);
  }

  @Override
  public <E> void insertWithDeferredIdAllocation(E entity, Type entityType) {
    try {
      DatastoreUtils.validateDeferredIdAllocation(entity, entityType);
      FullEntity<?> nativeEntity = (FullEntity<?>) Marshaller.marshal(entityManager, entity,
              Intent.INSERT, entityType);
      nativeBatch.addWithDeferredIdAllocation(nativeEntity);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  @Override
  public <E> void insertWithDeferredIdAllocation(List<E> entities) {
    insertWithDeferredIdAllocation(entities, null);
  }

  @Override
  public <E> void insertWithDeferredIdAllocation(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    try {
      DatastoreUtils.validateDeferredIdAllocation(entities.get(0), entityType);
      FullEntity<?>[] nativeEntities = DatastoreUtils.toNativeFullEntities(entities, entityManager,
          Intent.INSERT, entityType);
      nativeBatch.addWithDeferredIdAllocation(nativeEntities);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }

  }

  @Override
  public <E> E update(E entity) {
    return update(entity, null);
  }

  @Override
  public <E> E update(E entity, Type entityType) {
    return writer.update(entity, entityType);
  }

  @Override
  public <E> List<E> update(List<E> entities) {
    return update(entities, null);
  }

  @Override
  public <E> List<E> update(List<E> entities, Type entityType) {
    return writer.update(entities, entityType);
  }

  @Override
  public <E> E upsert(E entity) {
    return upsert(entity, null);
  }

  @Override
  public <E> E upsert(E entity, Type entityType) {
    return writer.upsert(entity, entityType);
  }

  @Override
  public <E> List<E> upsert(List<E> entities) {
    return upsert(entities, null);
  }

  @Override
  public <E> List<E> upsert(List<E> entities, Type entityType) {
    return writer.upsert(entities, entityType);
  }

  @Override
  public <E> void upsertWithDeferredIdAllocation(E entity) {
    upsertWithDeferredIdAllocation(entity, null);
  }

  @Override
  public <E> void upsertWithDeferredIdAllocation(E entity, Type entityType) {
    try {
      DatastoreUtils.validateDeferredIdAllocation(entity, entityType);
      FullEntity<?> nativeEntity = (FullEntity<?>) Marshaller.marshal(entityManager, entity,
              Intent.UPSERT, entityType);
      nativeBatch.putWithDeferredIdAllocation(nativeEntity);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }

  }

  @Override
  public <E> void upsertWithDeferredIdAllocation(List<E> entities) {
    upsertWithDeferredIdAllocation(entities, null);
  }

  @Override
  public <E> void upsertWithDeferredIdAllocation(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    try {
      DatastoreUtils.validateDeferredIdAllocation(entities.get(0), entityType);
      FullEntity<?>[] nativeEntities = DatastoreUtils.toNativeFullEntities(entities, entityManager,
          Intent.UPSERT, entityType);
      nativeBatch.putWithDeferredIdAllocation(nativeEntities);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }

  }

  @Override
  public void delete(Object entity) {
    delete(entity, null);
  }

  @Override
  public void delete(Object entity, Type entityType) {
    writer.delete(entity, entityType);
  }

  @Override
  public void delete(List<?> entities) {
    delete(entities, null);
  }

  @Override
  public void delete(List<?> entities, Type entityType) {
    writer.delete(entities, entityType);
  }

  @Override
  public <E> void delete(Type entityType, long id) {
    writer.delete(entityType, id);
  }

  @Override
  public <E> void delete(Type entityType, String id) {
    writer.delete(entityType, id);
  }

  @Override
  public <E> void delete(Type entityType, DatastoreKey parentKey, long id) {
    writer.delete(entityType, parentKey, id);
  }

  @Override
  public <E> void delete(Type entityType, DatastoreKey parentKey, String id) {
    writer.delete(entityType, parentKey, id);
  }

  @Override
  public void deleteByKey(DatastoreKey key) {
    writer.deleteByKey(key);
  }

  @Override
  public void deleteByKey(List<DatastoreKey> keys) {
    writer.deleteByKey(keys);
  }

  @Override
  public boolean isActive() {
    return nativeBatch.isActive();
  }

  @Override
  public Response submit() {
    try {
      Batch.Response nativeResponse = nativeBatch.submit();
      return new DefaultResponse(nativeResponse);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Implementation of {@link com.jmethods.catatumbo.DatastoreBatch.Response}.
   * 
   * @author Sai Pullabhotla
   *
   */
  static class DefaultResponse implements Response {

    /**
     * Native response
     */
    private final Batch.Response nativeResponse;

    /**
     * Creates a new instance of <code>DefaultResponse</code>.
     * 
     * @param nativeResponse
     *          the native response
     */
    public DefaultResponse(Batch.Response nativeResponse) {
      this.nativeResponse = nativeResponse;
    }

    @Override
    public List<DatastoreKey> getGeneratedKeys() {
      return DatastoreUtils.toDatastoreKeys(nativeResponse.getGeneratedKeys());
    }
  }

}
