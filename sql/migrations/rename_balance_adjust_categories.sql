-- 将两个同名的“余额调整”分类改为可区分名称：
--   收入调整（type=2） -> 余额调整收入
--   支出调整（type=1） -> 余额调整支出
-- 分类按 icon='adjust' 识别，只改名称，不影响已有交易和余额。
USE budgetpilot;

UPDATE t_category SET name = '余额调整收入' WHERE icon = 'adjust' AND type = 2;
UPDATE t_category SET name = '余额调整支出' WHERE icon = 'adjust' AND type = 1;

-- 校验
SELECT id, user_id, name, type, icon FROM t_category WHERE icon = 'adjust' ORDER BY user_id, type;