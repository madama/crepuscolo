
    create table task (
        id varchar(255) not null,
        version bigint not null,
        complete boolean,
        creationDate bigint not null,
        description varchar(255),
        lastModified bigint not null,
        name varchar(255) not null,
        priority varchar(255) not null,
        user_id varchar(255),
        primary key (id)
    );

    create table tasklist (
        id varchar(255) not null,
        version bigint not null,
        complete boolean,
        creationDate bigint not null,
        lastModified bigint not null,
        name varchar(255) not null,
        user_id varchar(255),
        primary key (id)
    );

    create table tasklist_task (
        tasklist_id varchar(255) not null,
        tasks_id varchar(255) not null
    );

    create table user (
        id varchar(255) not null,
        version bigint not null,
        creationDate bigint not null,
        description varchar(255),
        email varchar(255) not null,
        lastModified bigint not null,
        password varchar(255) not null,
        tokenSalt varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    );

    alter table tasklist_task 
        add constraint UK_fhqj3u6606a1nksxp5l3cu64  unique (tasks_id);

    alter table user 
        add constraint UK_ob8kqyqqgmefl0aco34akdtpe  unique (email);

    alter table task 
        add constraint FK_4fmjedju7b35tb5cr71n3ntb0 
        foreign key (user_id) 
        references user;

    alter table tasklist 
        add constraint FK_hodpdl33lpsx5jjf38j4rr3bi 
        foreign key (user_id) 
        references user;

    alter table tasklist_task 
        add constraint FK_fhqj3u6606a1nksxp5l3cu64 
        foreign key (tasks_id) 
        references task;

    alter table tasklist_task 
        add constraint FK_j470a8yyc7bbj46nriry3dfwu 
        foreign key (tasklist_id) 
        references tasklist;
