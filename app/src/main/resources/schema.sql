-- Table for storing click logs
CREATE TABLE click (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, -- Auto-incrementing primary key
    hash VARCHAR(255) NOT NULL,           -- Foreign key referencing shorturl.hash
    created TIMESTAMP WITH TIME ZONE NOT NULL,           -- Timestamp of the click
    ip VARCHAR(45),                       -- IP address (supports IPv4 and IPv6)
    referrer VARCHAR(255),                -- Referrer URL
    browser VARCHAR(255),                 -- Browser information
    platform VARCHAR(255),                -- Platform information
    country VARCHAR(255)                  -- Country code
);

-- Table for storing short URLs
CREATE TABLE shorturl (
    hash VARCHAR(255) PRIMARY KEY,        -- Hash as primary key
    target VARCHAR(2083) NOT NULL,        -- Target URL (max length for URLs)
    created TIMESTAMP WITH TIME ZONE NOT NULL,           -- Timestamp when the short URL was created
    owner VARCHAR(255),                   -- Owner of the short URL
    mode INT NOT NULL,                    -- Redirection mode (e.g., 301 or 302)
    safe BOOLEAN NOT NULL,                -- Indicates whether the URL is safe
    ip VARCHAR(45),                       -- IP address of the creator
    country VARCHAR(255)                  -- Country code of the creator
);

-- Index for optimizing queries by hash in the click table
CREATE INDEX idx_click_hash ON click (hash);
