package com.bootcamp_proj.bootcampproj.psql_brt_abonents;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BrtAbonentsService {
    @Autowired
    private BrtAbonentsRepository brtAbonentsRepository;

    public BrtAbonentsService() {}

    public Iterable<BrtAbonents> findAll() {
        return brtAbonentsRepository.findAll();
    }
}
