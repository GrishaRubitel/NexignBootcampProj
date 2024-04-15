package com.bootcamp_proj.bootcampproj.psql_cdr_abonents;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrAbonentsService {
    @Autowired
    private CdrAbonentsRepository cdrAbonentsRepository;

    public CdrAbonentsService() {}

    public Iterable<CdrAbonents> findAll() {
        return cdrAbonentsRepository.findAll();
    }
}
