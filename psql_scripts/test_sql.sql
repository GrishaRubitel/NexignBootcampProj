---Тест на проверку параллельных звонков
select msisdn, msisdn_to, unix_start, unix_end, unix_end - lead(unix_start) over (partition by msisdn order by unix_start desc) as call_len
from transactions t
order by msisdn, unix_start asc
---Пояснение
---Если значение в атрибуте call_len отрицательно и не null - один абонент совершает два звонка
---в одно и то же время
---Если значение в этом же атрибуте положитетельно - то всё ок, параллельностей нет