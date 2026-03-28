
# @formatter:off
update t_page_view set ip_area = '*' where ip_area = 'UNKNOWN';
update t_page_view set ip_city = '*' where ip_city = 'UNKNOWN';
update t_page_view set ip_isp = '*' where ip_isp = 'UNKNOWN';
update t_page_view set ip_province = '*' where ip_province = 'UNKNOWN';
update t_page_view set ip_country = '*' where ip_country = 'UNKNOWN';

update logging set ip_area = '*' where ip_area = 'UNKNOWN';
update logging set ip_city = '*' where ip_city = 'UNKNOWN';
update logging set ip_isp = '*' where ip_isp = 'UNKNOWN';
update logging set ip_province = '*' where ip_province = 'UNKNOWN';
update logging set ip_country = '*' where ip_country = 'UNKNOWN';
