-- ---------------------------------------------------------------------------
-- Story dependencies (one-direction) and related stories (non-blocking)
-- Ref: USER-STORY.md — dependencies stored one direction; relatedStories non-blocking
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `story_dependencies` (
  `story_id`           VARCHAR(16) NOT NULL COMMENT 'story that depends',
  `depends_on_story_id` VARCHAR(16) NOT NULL COMMENT 'story that must reach Done first',
  `created_at`         DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (`story_id`, `depends_on_story_id`),
  KEY `ix_story_dependencies_depends_on` (`depends_on_story_id`),
  CONSTRAINT `fk_story_deps_story` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_story_deps_depends_on` FOREIGN KEY (`depends_on_story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_story_deps_no_self` CHECK (`story_id` != `depends_on_story_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `story_related` (
  `story_id`        VARCHAR(16) NOT NULL,
  `related_story_id` VARCHAR(16) NOT NULL,
  `created_at`      DATETIME(3) NOT NULL DEFAULT (CURRENT_TIMESTAMP(3)),
  PRIMARY KEY (`story_id`, `related_story_id`),
  KEY `ix_story_related_related` (`related_story_id`),
  CONSTRAINT `fk_story_related_story` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_story_related_related` FOREIGN KEY (`related_story_id`) REFERENCES `stories` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_story_related_no_self` CHECK (`story_id` != `related_story_id`),
  CONSTRAINT `chk_story_related_ordered` CHECK (`story_id` < `related_story_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
