-- Table for storing short URLs
CREATE TABLE shorturl (
    hash VARCHAR(255) PRIMARY KEY,              -- Hash as primary key
    target VARCHAR(2083) NOT NULL,              -- Target URL (max length for URLs)
    created TIMESTAMP WITH TIME ZONE NOT NULL,  -- Timestamp when the short URL was created
    mode INT NOT NULL,                          -- Redirection mode (e.g., 301 or 302)
    ip VARCHAR(45),                             -- IP address of the creator
    country VARCHAR(255),                       -- Country code of the creator
    reachable BOOLEAN NOT NULL,                 -- Indicates whether the URL is reachable
    safe BOOLEAN NOT NULL,                      -- Indicates whether the URL is safe
    validated BOOLEAN NOT NULL                  -- If the URL is being validated
);

-- Table for storing click logs
CREATE TABLE click (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,       -- Auto-incrementing primary key
    hash VARCHAR(255) NOT NULL,                 -- Foreign key referencing shorturl.hash
    created TIMESTAMP WITH TIME ZONE NOT NULL,  -- Timestamp of the click
    ip VARCHAR(45),                             -- IP address (supports IPv4 and IPv6)
    browser VARCHAR(255),                       -- Browser information
    platform VARCHAR(255),                      -- Platform information
    country VARCHAR(255),                       -- Country code
    CONSTRAINT fk_shorturl FOREIGN KEY (hash) REFERENCES shorturl(hash)
);

-- Additional indices for common query patterns
CREATE INDEX idx_click_hash_created ON click (hash, created); -- Optimize queries filtering by hash and created
CREATE INDEX idx_shorturl_created ON shorturl (created);      -- Optimize queries filtering or sorting by created
