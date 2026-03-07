-- ---------------------------------------------------------------------------
-- Data: Insert story dependencies (one-direction: story depends on depends_on)
-- Ref: USER-STORIES.md — dependencies field
-- Run from trace_hq:  ./scripts/mysql.sh < database/data/05_story_dependencies.sql
-- Requires: 04_stories.sql applied first.
-- ---------------------------------------------------------------------------

INSERT INTO `story_dependencies` (`story_id`, `depends_on_story_id`) VALUES
('S004', 'S003'),
('S005', 'S001'),
('S006', 'S001'),
('S011', 'S001'),
('S012', 'S003'),
('S013', 'S012'),
('S014', 'S001'),
('S014', 'S005'),
('S015', 'S001'),
('S015', 'S005'),
('S016', 'S015'),
('S017', 'S003'),
('S018', 'S005'),
('S019', 'S006'),
('S021', 'S007'),
('S023', 'S007');
