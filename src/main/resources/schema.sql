create table if not exists MANAGER_PARAM
(
    ID          BIGINT not null primary key,
    PARAM_NAME  CHARACTER VARYING(255),
    PARAM_VALUE CHARACTER VARYING(255)
);

create table if not exists DEVICE_PARAM
(
    ID          BIGINT not null primary key,
    DEVICE_ID   INTEGER,
    DEVICE_NAME CHARACTER VARYING(255),
    PARAM_NAME  CHARACTER VARYING(255),
    PARAM_VALUE CHARACTER VARYING(255)
);

create table if not exists PROGRAM_PARAM
(
    ID          BIGINT not null primary key,
    PROGRAM_ID  INTEGER,
    PARAM_NAME  CHARACTER VARYING(255),
    PARAM_VALUE CHARACTER VARYING(255)
);

create table if not exists RESTORE_STEP
(
    ID     BIGINT not null primary key,
    TIME   TIMESTAMP,
    QUERY  CHARACTER VARYING(255),
    PARAMS CHARACTER VARYING(255)
);

create table if not exists TASK_PROCESS
(
    STAGE_ID  BIGINT not null primary key,
    ENTITY_ID BIGINT,
    OPERATION CHARACTER VARYING(255)
);

create sequence if not exists MANAGER_PARAM_SEQ START WITH 1 INCREMENT BY 1;
create sequence if not exists DEVICE_PARAM_SEQ START WITH 1 INCREMENT BY 1;
create sequence if not exists PROGRAM_PARAM_SEQ START WITH 1 INCREMENT BY 1;
create sequence if not exists RESTORE_STEP_SEQ START WITH 1 INCREMENT BY 1;






