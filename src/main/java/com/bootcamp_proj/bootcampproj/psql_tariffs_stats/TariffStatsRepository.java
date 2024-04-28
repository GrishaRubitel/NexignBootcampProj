package com.bootcamp_proj.bootcampproj.psql_tariffs_stats;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffStatsRepository extends CrudRepository<TariffStats, String> {
}
