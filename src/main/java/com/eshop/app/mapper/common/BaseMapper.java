package com.eshop.app.mapper.common;

import java.util.List;
import java.util.Set;

/**
 * Generic base mapper interface providing common mapping operations.
 */
public interface BaseMapper<E, D, C, U> {

    D toDto(E entity);

    E toEntity(C createRequest);

    void updateEntityFromDto(U updateRequest, @org.mapstruct.MappingTarget E entity);

    List<D> toDtoList(List<E> entities);

    Set<D> toDtoSet(Set<E> entities);
}
