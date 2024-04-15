package com.bootcamp_proj.bootcampproj.psql_transactions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
