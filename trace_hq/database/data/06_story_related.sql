-- ---------------------------------------------------------------------------
-- Data: Insert story_related (non-blocking links; story_id < related_story_id)
-- Ref: USER-STORIES.md — relatedStories field
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/06_story_related.sql
-- Requires: 04_stories.sql applied first.
-- ---------------------------------------------------------------------------

INSERT INTO `story_related` (`story_id`, `related_story_id`) VALUES
('S012', 'S013'),
('S015', 'S016');
