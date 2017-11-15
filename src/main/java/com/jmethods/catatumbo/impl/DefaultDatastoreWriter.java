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

import static com.jmethods.catatumbo.impl.DatastoreUtils.rollbackIfActive;
import static com.jmethods.catatumbo.impl.DatastoreUtils.toEntities;
import static com.jmethods.catatumbo.impl.DatastoreUtils.toNativeEntities;
import static com.jmethods.catatumbo.impl.DatastoreUtils.toNativeFullEntities;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.cloud.datastore.Batch;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.EntityManagerException;
import com.jmethods.catatumbo.OptimisticLockException;
import com.jmethods.catatumbo.impl.Marshaller.Intent;

/**
 * Worker class for performing write operations on the Cloud Datastore.
 * 
 * @author Sai Pullabhotla
 *
 */
public class DefaultDatastoreWriter {

  /**
   * A reference to the entity manager
   */
  private DefaultEntityManager entityManager;

  /**
   * Reference to the native DatastoreWriter for updating the Cloud Datastore. This could be the
   * {@link Datastore}, {@link Transaction} or {@link Batch}.
   */
  private DatastoreWriter nativeWriter;

  /**
   * A reference to the Datastore
   */
  private Datastore datastore;

  /**
   * Creates a new instance of <code>DefaultDatastoreWriter</code>.
   * 
   * @param entityManager
   *          a reference to the entity manager.
   */
  public DefaultDatastoreWriter(DefaultEntityManager entityManager) {
    this.entityManager = entityManager;
    this.datastore = entityManager.getDatastore();
    this.nativeWriter = datastore;
  }

  /**
   * Creates a new instance of <code>DefaultDatastoreWriter</code> for executing batch updates.
   * 
   * @param batch
   *          the {@link DefaultDatastoreBatch}.
   */
  public DefaultDatastoreWriter(DefaultDatastoreBatch batch) {
    this.entityManager = batch.getEntityManager();
    this.datastore = entityManager.getDatastore();
    this.nativeWriter = batch.getNativeBatch();
  }

  /**
   * Creates a new instance of <code>DefaultDatastoreWriter</code> for transactional updates.
   * 
   * @param transaction
   *          the {@link DefaultDatastoreTransaction}.
   */
  public DefaultDatastoreWriter(DefaultDatastoreTransaction transaction) {
    this.entityManager = transaction.getEntityManager();
    this.datastore = entityManager.getDatastore();
    this.nativeWriter = transaction.getNativeTransaction();
  }

  /**
   * Inserts the given entity into the Cloud Datastore.
   * 
   * @param entity
   *          the entity to insert
   * @return the inserted entity. The inserted entity will not be same as the passed in entity. For
   *         example, the inserted entity may contain any generated ID, key, parent key, etc.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  public <E> E insert(E entity, Type entityType) {
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_INSERT, entity, entityType);
      FullEntity<?> nativeEntity = (FullEntity<?>) Marshaller.marshal(entityManager, entity,
              Intent.INSERT, entityType);
      Type entityClass = entityType != null ? entityType : entity.getClass();
      Entity insertedNativeEntity = nativeWriter.add(nativeEntity);
      @SuppressWarnings("unchecked")
      E insertedEntity = (E) Unmarshaller.unmarshal(insertedNativeEntity, entityClass);
      entityManager.executeEntityListeners(CallbackType.POST_INSERT, insertedEntity, entityType);
      return insertedEntity;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Inserts the given list of entities into the Cloud Datastore.
   * 
   * @param entities
   *          the entities to insert.
   * @return the inserted entities. The inserted entities will not be same as the passed in
   *         entities. For example, the inserted entities may contain generated ID, key, parent key,
   *         etc.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  @SuppressWarnings("unchecked")
  public <E> List<E> insert(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_INSERT, entities, entityType);
      FullEntity<?>[] nativeEntities = toNativeFullEntities(entities, entityManager,
              Intent.INSERT, entityType);
      Type entityClass = entityType != null ? entityType : entities.get(0).getClass();
      List<Entity> insertedNativeEntities = nativeWriter.add(nativeEntities);
      List<E> insertedEntities = (List<E>) toEntities(entityClass, insertedNativeEntities);
      entityManager.executeEntityListeners(CallbackType.POST_INSERT, insertedEntities, entityType);
      return insertedEntities;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Updates the given entity in the Cloud Datastore. The passed in Entity must have its ID set for
   * the update to work.
   * 
   * @param entity
   *          the entity to update
   * @return the updated entity.
   * @throws EntityManagerException
   *           if any error occurs while updating.
   */
  @SuppressWarnings("unchecked")
  public <E> E update(E entity, Type entityType) {
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_UPDATE, entity, entityType);
      Intent intent = (nativeWriter instanceof Batch) ? Intent.BATCH_UPDATE : Intent.UPDATE;
      Entity nativeEntity = (Entity) Marshaller.marshal(entityManager, entity, intent, entityType);
      Type entityClass = entityType != null ? entityType : entity.getClass();
      nativeWriter.update(nativeEntity);
      E updatedEntity = (E) Unmarshaller.unmarshal(nativeEntity, entityClass);
      entityManager.executeEntityListeners(CallbackType.POST_UPDATE, updatedEntity, entityType);
      return updatedEntity;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }

  }

  /**
   * Updates the given list of entities in the Cloud Datastore.
   * 
   * @param entities
   *          the entities to update. The passed in entities must have their ID set for the update
   *          to work.
   * @return the updated entities
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  @SuppressWarnings("unchecked")
  public <E> List<E> update(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }
    try {
      Type entityClass = entityType != null ? entityType : entities.get(0).getClass();
      entityManager.executeEntityListeners(CallbackType.PRE_UPDATE, entities, entityType);
      Intent intent = (nativeWriter instanceof Batch) ? Intent.BATCH_UPDATE : Intent.UPDATE;
      Entity[] nativeEntities = toNativeEntities(entities, entityManager, intent, entityType);
      nativeWriter.update(nativeEntities);
      List<E> updatedEntities = toEntities(entityClass, nativeEntities);
      entityManager.executeEntityListeners(CallbackType.POST_UPDATE, updatedEntities, entityType);
      return updatedEntities;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Updates the given entity with optimistic locking, if the entity is set up to support optimistic
   * locking. Otherwise, a normal update is performed.
   * 
   * @param entity
   *          the entity to update
   * @return the updated entity which may be different than the given entity.
   */
  public <E> E updateWithOptimisticLock(E entity, Type entityType) {
    PropertyMetadata versionMetadata = EntityIntrospector.getVersionMetadata(entity, entityType);
    if (versionMetadata == null) {
      return update(entity, entityType);
    } else {
      return updateWithOptimisticLockingInternal(entity, versionMetadata, entityType);
    }

  }

  /**
   * Updates the given list of entities using optimistic locking feature, if the entities are set up
   * to support optimistic locking. Otherwise, a normal update is performed.
   * 
   * @param entities
   *          the entities to update
   * @return the updated entities
   */
  public <E> List<E> updateWithOptimisticLock(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }
    Type entityClass = entityType != null ? entityType : entities.get(0).getClass();

    PropertyMetadata versionMetadata = EntityIntrospector.getVersionMetadata(entityClass);
    if (versionMetadata == null) {
      return update(entities, entityType);
    } else {
      return updateWithOptimisticLockInternal(entities, versionMetadata, entityType);
    }
  }

  /**
   * Worker method for updating the given entity with optimistic locking.
   * 
   * @param entity
   *          the entity to update
   * @param versionMetadata
   *          the metadata for optimistic locking
   * @return the updated entity
   */
  @SuppressWarnings("unchecked")
  private <E> E updateWithOptimisticLockingInternal(E entity,
                                                    PropertyMetadata versionMetadata,
                                                    Type entityType) {
    Transaction transaction = null;
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_UPDATE, entity, entityType);
      Entity nativeEntity = (Entity) Marshaller.marshal(entityManager, entity,
              Intent.UPDATE, entityType);
      transaction = datastore.newTransaction();
      Entity storedNativeEntity = transaction.get(nativeEntity.getKey());
      if (storedNativeEntity == null) {
        throw new OptimisticLockException(
            String.format("Entity does not exist: %s", nativeEntity.getKey()));
      }
      String versionPropertyName = versionMetadata.getMappedName();
      long version = nativeEntity.getLong(versionPropertyName) - 1;
      long storedVersion = storedNativeEntity.getLong(versionPropertyName);
      if (version != storedVersion) {
        throw new OptimisticLockException(
            String.format("Expecting version %d, but found %d", version, storedVersion));
      }
      transaction.update(nativeEntity);
      transaction.commit();
      Type entityClass = entityType != null ? entityType : entity.getClass();
      E updatedEntity = (E) Unmarshaller.unmarshal(nativeEntity, entityClass);
      entityManager.executeEntityListeners(CallbackType.POST_UPDATE, updatedEntity, entityType);
      return updatedEntity;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    } finally {
      rollbackIfActive(transaction);
    }
  }

  /**
   * Internal worker method for updating the entities using optimistic locking.
   * 
   * @param entities
      Entity[] nativeEntities = toNativeEntities(entities, entityManager, intent, type);
   *          the entities to update
   * @param versionMetadata
   *          the metadata of the version property
   * @return the updated entities
   */
  @SuppressWarnings("unchecked")
  private <E> List<E> updateWithOptimisticLockInternal(List<E> entities,
                                                       PropertyMetadata versionMetadata,
                                                       Type entityType) {
    Transaction transaction = null;
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_UPDATE, entities, entityType);
      Entity[] nativeEntities = toNativeEntities(entities, entityManager,
              Intent.UPDATE, entityType);
      // The above native entities already have the version incremented by
      // the marshalling process
      Key[] nativeKeys = new Key[nativeEntities.length];
      for (int i = 0; i < nativeEntities.length; i++) {
        nativeKeys[i] = nativeEntities[i].getKey();
      }
      transaction = datastore.newTransaction();
      List<Entity> storedNativeEntities = transaction.fetch(nativeKeys);
      String versionPropertyName = versionMetadata.getMappedName();

      for (int i = 0; i < nativeEntities.length; i++) {
        long version = nativeEntities[i].getLong(versionPropertyName) - 1;
        Entity storedNativeEntity = storedNativeEntities.get(i);
        if (storedNativeEntity == null) {
          throw new OptimisticLockException(
              String.format("Entity does not exist: %s", nativeKeys[i]));
        }
        long storedVersion = storedNativeEntities.get(i).getLong(versionPropertyName);
        if (version != storedVersion) {
          throw new OptimisticLockException(
              String.format("Expecting version %d, but found %d", version, storedVersion));
        }
      }
      transaction.update(nativeEntities);
      transaction.commit();
      Type entityClass = entityType != null ? entityType : entities.get(0).getClass();
      List<E> updatedEntities = (List<E>) toEntities(entityClass, nativeEntities);
      entityManager.executeEntityListeners(CallbackType.POST_UPDATE, updatedEntities, entityType);
      return updatedEntities;

    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    } finally {
      rollbackIfActive(transaction);
    }

  }

  /**
   * Updates or inserts the given entity in the Cloud Datastore. If the entity does not have an ID,
   * it may be generated.
   * 
   * @param entity
   *          the entity to update or insert
   * @return the updated/inserted entity.
   * @throws EntityManagerException
   *           if any error occurs while saving.
   */
  public <E> E upsert(E entity, Type entityType) {
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_UPSERT, entity, entityType);
      FullEntity<?> nativeEntity = (FullEntity<?>) Marshaller.marshal(entityManager, entity,
              Intent.UPSERT, entityType);
      Entity upsertedNativeEntity = nativeWriter.put(nativeEntity);
      @SuppressWarnings("unchecked")
      Type entityClass = entityType != null ? entityType : entity.getClass();

      E upsertedEntity = (E) Unmarshaller.unmarshal(upsertedNativeEntity, entityClass);
      entityManager.executeEntityListeners(CallbackType.POST_UPSERT, upsertedEntity, entityType);
      return upsertedEntity;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Updates or inserts the given list of entities in the Cloud Datastore. If the entities do not
   * have a valid ID, IDs may be generated.
   * 
   * @param entities
   *          the entities to update/or insert.
   * @return the updated or inserted entities
   * @throws EntityManagerException
   *           if any error occurs while saving.
   */
  @SuppressWarnings("unchecked")
  public <E> List<E> upsert(List<E> entities, Type entityType) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_UPSERT, entities, entityType);
      FullEntity<?>[] nativeEntities = toNativeFullEntities(entities, entityManager,
              Intent.UPSERT, entityType);
      Type entityClass = entityType != null ? entityType : entities.get(0).getClass();
      List<Entity> upsertedNativeEntities = nativeWriter.put(nativeEntities);
      List<E> upsertedEntities = (List<E>) toEntities(entityClass, upsertedNativeEntities);
      entityManager.executeEntityListeners(CallbackType.POST_UPSERT, upsertedEntities, entityType);
      return upsertedEntities;
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the given entity from the Cloud Datastore.
   * 
   * @param entity
   *          the entity to delete. The entity must have it ID set for the deletion to succeed.
   * @throws EntityManagerException
   *           if any error occurs while deleting.
   */
  public void delete(Object entity, Type entityType) {
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_DELETE, entity, entityType);
      Key nativeKey = Marshaller.marshalKey(entityManager, entity, entityType);
      nativeWriter.delete(nativeKey);
      entityManager.executeEntityListeners(CallbackType.POST_DELETE, entity, entityType);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the given entities from the Cloud Datastore.
   * 
   * @param entities
   *          the entities to delete. The entities must have it ID set for the deletion to succeed.
   * @throws EntityManagerException
   *           if any error occurs while deleting.
   */
  public void delete(List<?> entities, Type entityType) {
    try {
      entityManager.executeEntityListeners(CallbackType.PRE_DELETE, entities, entityType);
      Key[] nativeKeys = new Key[entities.size()];
      for (int i = 0; i < entities.size(); i++) {
        nativeKeys[i] = Marshaller.marshalKey(entityManager, entities.get(i), entityType);
      }
      nativeWriter.delete(nativeKeys);
      entityManager.executeEntityListeners(CallbackType.POST_DELETE, entities, entityType);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the entity with the given ID. The entity is assumed to be a root entity (no parent).
   * The entity kind will be determined from the supplied entity class.
   * 
   * @param entityType
   *          the entity type.
   * @param id
   *          the ID of the entity.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  public <E> void delete(Type entityType, long id) {
    try {
      EntityMetadata entityMetadata = EntityIntrospector.introspect(entityType);
      Key nativeKey = entityManager.newNativeKeyFactory().setKind(entityMetadata.getKind())
          .newKey(id);
      nativeWriter.delete(nativeKey);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the entity with the given ID. The entity is assumed to be a root entity (no parent).
   * The entity kind will be determined from the supplied entity class.
   * 
   * @param entityType
   *          the entity type.
   * @param id
   *          the ID of the entity.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  public <E> void delete(Type entityType, String id) {
    try {
      EntityMetadata entityMetadata = EntityIntrospector.introspect(entityType);
      Key nativeKey = entityManager.newNativeKeyFactory().setKind(entityMetadata.getKind())
          .newKey(id);
      nativeWriter.delete(nativeKey);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the entity with the given ID and parent key.
   * 
   * @param entityType
   *          the entity type.
   * @param parentKey
   *          the parent key
   * @param id
   *          the ID of the entity.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  public <E> void delete(Type entityType, DatastoreKey parentKey, long id) {
    try {
      EntityMetadata entityMetadata = EntityIntrospector.introspect(entityType);
      Key nativeKey = Key.newBuilder(parentKey.nativeKey(), entityMetadata.getKind(), id).build();
      nativeWriter.delete(nativeKey);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the entity with the given ID and parent key.
   * 
   * @param entityType
   *          the entity type.
   * @param parentKey
   *          the parent key
   * @param id
   *          the ID of the entity.
   * @throws EntityManagerException
   *           if any error occurs while inserting.
   */
  public <E> void delete(Type entityType, DatastoreKey parentKey, String id) {
    try {
      EntityMetadata entityMetadata = EntityIntrospector.introspect(entityType);
      Key nativeKey = Key.newBuilder(parentKey.nativeKey(), entityMetadata.getKind(), id).build();
      nativeWriter.delete(nativeKey);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes an entity given its key.
   * 
   * @param key
   *          the entity's key
   * @throws EntityManagerException
   *           if any error occurs while deleting.
   */
  public void deleteByKey(DatastoreKey key) {
    try {
      nativeWriter.delete(key.nativeKey());
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

  /**
   * Deletes the entities having the given keys.
   * 
   * @param keys
   *          the entities' keys
   * @throws EntityManagerException
   *           if any error occurs while deleting.
   */
  public void deleteByKey(List<DatastoreKey> keys) {
    try {
      Key[] nativeKeys = new Key[keys.size()];
      for (int i = 0; i < keys.size(); i++) {
        nativeKeys[i] = keys.get(i).nativeKey();
      }
      nativeWriter.delete(nativeKeys);
    } catch (DatastoreException exp) {
      throw DatastoreUtils.wrap(exp);
    }
  }

}
