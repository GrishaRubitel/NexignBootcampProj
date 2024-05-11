package com.bootcamp_proj.bootcampproj.psql_hrs_user_minutes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ORM сервис для выполнения запросов к таблице "users_minutes" базы данных оператора "Ромашки"
 */
@Service
public class UserMinutesService {
    @Autowired
    UserMinutesRepository userMinutesRepository;

    public UserMinutesService() {}

    public void saveUserMinutes(UserMinutes userMinutes) {
        userMinutesRepository.save(userMinutes);
    }
}
