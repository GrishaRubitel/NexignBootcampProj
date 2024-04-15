package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrtAbonentsRepository extends CrudRepository<BrtAbonents, Long> {
}
