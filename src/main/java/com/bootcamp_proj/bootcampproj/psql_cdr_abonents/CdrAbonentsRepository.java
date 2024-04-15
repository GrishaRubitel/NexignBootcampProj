package com.bootcamp_proj.bootcampproj.psql_cdr_abonents;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdrAbonentsRepository extends CrudRepository<CdrAbonents, Long> {

}
