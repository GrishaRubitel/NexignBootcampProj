package com.bootcamp_proj.bootcampproj.psql_transactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ORM сервис для работы с таблицей "transactions" базы данных генератора звонков
 */
@Service
public class TransactionService {
    @Autowired
    TransactionRepository transactionRepository;

    public TransactionService() {}

    public void insertRecord(Transaction rec) {
        transactionRepository.save(rec);
    }

    public void trunkTable() {
        transactionRepository.deleteAll();
    }
}
