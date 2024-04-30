package com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMinutesRepository extends CrudRepository<UserMinutes, Long> {
}
