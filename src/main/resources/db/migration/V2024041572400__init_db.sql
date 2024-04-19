CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS "users" (
   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
   user_name VARCHAR(255) UNIQUE NOT NULL,
   email VARCHAR(255) UNIQUE NOT NULL,
   password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    size BIGINT,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modification_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    owner_id UUID REFERENCES "users"(id),
    checksum VARCHAR(64),
    storage_location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES document(id),
    key VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    CONSTRAINT fk_document_id FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS permissions (
   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
   document_id UUID REFERENCES document(id),
   user_id UUID REFERENCES "users"(id),
   permission VARCHAR(10) NOT NULL CHECK (permission IN ('READ', 'WRITE', 'DELETE', 'SHARE', 'ALL')),
   CONSTRAINT fk_doc_id FOREIGN KEY (document_id) REFERENCES document(id) ON DELETE CASCADE,
   CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES "users"(id) ON DELETE CASCADE
);
