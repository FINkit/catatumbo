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

import static com.jmethods.catatumbo.impl.DatastoreUtils.toNativeFullEntities;

import java.lang.reflect.Type;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Transaction;
import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.DatastoreTransaction;
import com.jmethods.catatumbo.EntityManagerException;
import com.jmethods.catatumbo.EntityQueryRequest;
import com.jmethods.catatumbo.KeyQueryRequest;
import com.jmethods.catatumbo.ProjectionQueryRequest;
import com.jmethods.catatumbo.QueryResponse;
import com.jmethods.catatumbo.impl.Marshaller.Intent;

/**
 * Default implementation of the {@link DatastoreTransaction} interface.
 * 
 * @author Sai Pullabhotla
 *
 */
public class DefaultDatastoreTransaction implements DatastoreTransaction {

  /**
   * Entity manager that created this transaction
   */
  private DefaultEntityManager entityManager;

  /**
   * Native transaction
   */
  private Transaction nativeTransaction;

  /**
   * Datastore
   */
  private Datastore datastore;

  /**
   * Reader
   */
  private DefaultDatastoreReader reader;

  /**
   * Writer
   */
  private DefaultDatastoreWriter writer;

  /**
   * Creates a new instance of <code>DatastoreTransaction</code>.
   * 
   * @param entityManager
   *          the entity manager that created this transaction.
   */
  public DefaultDatastoreTransaction(DefaultEntityManager entityManager) {
    this.entityManager = entityManager;
    this.datastore = entityManager.getDatastore();
    this.nativeTransaction = datastore.newTransaction();
    this.reader = new DefaultDatastoreReader(this);
    this.writer = new DefaultDatastoreWriter(this);
  }

  /**
   * Returns the entity manager that created this transaction.
   * 
   * @return the entity manager that created this transaction.
   */
  public DefaultEntityManager getEntityManager() {
    return entityManager;
  }

  /**
   * Returns the native transaction.
   * 
   * @return the native transaction.
   */
  public Transaction getNativeTransaction() {
    return nativeTransaction;
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
      nativeTransaction.addWithDeferredIdAllocation(nativeEntity);
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
      FullEntity<?>[] nativeEntities = toNativeFullEntities(entities, entityManager,
              Intent.INSERT, entityType);
      nativeTransaction.addWithDeferredIdAllocation(nativeEntities);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
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
      nativeTransaction.putWithDeferredIdAllocation(nativeEntity);
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
      FullEntity<?>[] nativeEntities = toNativeFullEntities(entities, entityManager,
              Intent.UPSERT, entityType);
      nativeTransaction.putWithDeferredIdAllocation(nativeEntities);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  @Override
  public boolean isActive() {
    try {
      return nativeTransaction.isActive();
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    } catch (Exception exp) {
      throw new EntityManagerException(exp);
    }
  }

  @Override
  public Response commit() {
    try {
      Transaction.Response nativeResponse = nativeTransaction.commit();
      return new DefaultResponse(nativeResponse);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    } catch (Exception exp) {
      throw new EntityManagerException(exp);
    }
  }

  @Override
  public void rollback() {
    try {
      nativeTransaction.rollback();
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    } catch (Exception exp) {
      throw new EntityManagerException(exp);
    }
  }

  /**
   * Transaction Response containing the results of a transaction commit.
   * 
   * @author Sai Pullabhotla
   *
   */
  static class DefaultResponse implements Response {

    /**
     * Native response
     */
    private final Transaction.Response nativeResponse;

    /**
     * Creates a new instance of <code>DefaultResponse</code>.
     * 
     * @param nativeResponse
     *          the native transaction response
     */
    public DefaultResponse(Transaction.Response nativeResponse) {
      this.nativeResponse = nativeResponse;
    }

    @Override
    public List<DatastoreKey> getGeneratedKeys() {
      return DatastoreUtils.toDatastoreKeys(nativeResponse.getGeneratedKeys());
    }
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
  public <E> E update(E entity) {
    return update(entity, null);
  }

  @Override
  public <E> E update(E entity, Type entityType) {
    return writer.updateWithOptimisticLock(entity, entityType);
  }

  @Override
  public <E> List<E> update(List<E> entities) {
    return update(entities, null);
  }

  @Override
  public <E> List<E> update(List<E> entities, Type entityType) {
    return writer.updateWithOptimisticLock(entities, entityType);
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
  public void delete(Type entityType, long id) {
    writer.delete(entityType, id);
  }

  @Override
  public void delete(Type entityType, String id) {
    writer.delete(entityType, id);
  }

  @Override
  public void delete(Type entityType, DatastoreKey parentKey, long id) {
    writer.delete(entityType, parentKey, id);
  }

  @Override
  public void delete(Type entityType, DatastoreKey parentKey, String id) {
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
  public <E> E load(Type entityType, long id) {
    return reader.load(entityType, id);
  }

  @Override
  public <E> E load(Type entityType, String id) {
    return reader.load(entityType, id);
  }

  @Override
  public <E> E load(Type entityType, DatastoreKey parentKey, long id) {
    return reader.load(entityType, parentKey, id);
  }

  @Override
  public <E> E load(Type entityType, DatastoreKey parentKey, String id) {
    return reader.load(entityType, parentKey, id);
  }

  @Override
  public <E> E load(Type entityType, DatastoreKey key) {
    return reader.load(entityType, key);
  }

  @Override
  public <E> List<E> loadById(Type entityType, List<Long> identifiers) {
    return reader.loadById(entityType, identifiers);
  }

  @Override
  public <E> List<E> loadByKey(Type entityType, List<DatastoreKey> keys) {
    return reader.loadByKey(entityType, keys);
  }

  @Override
  public <E> List<E> loadByName(Type entityType, List<String> identifiers) {
    return reader.loadByName(entityType, identifiers);
  }

  @Override
  public EntityQueryRequest createEntityQueryRequest(String query) {
    return reader.createEntityQueryRequest(query);
  }

  @Override
  public ProjectionQueryRequest createProjectionQueryRequest(String query) {
    return reader.createProjectionQueryRequest(query);
  }

  @Override
  public KeyQueryRequest createKeyQueryRequest(String query) {
    return reader.createKeyQueryRequest(query);
  }

  @Override
  public <E> QueryResponse<E> executeEntityQueryRequest(Type expectedResultType,
                                                        EntityQueryRequest request) {
    return reader.executeEntityQueryRequest(expectedResultType, request);
  }

  @Override
  public <E> QueryResponse<E> executeProjectionQueryRequest(Type expectedResultType,
      ProjectionQueryRequest request) {
    return reader.executeProjectionQueryRequest(expectedResultType, request);
  }

  @Override
  public QueryResponse<DatastoreKey> executeKeyQueryRequest(KeyQueryRequest request) {
    return reader.executeKeyQueryRequest(request);
  }

}
