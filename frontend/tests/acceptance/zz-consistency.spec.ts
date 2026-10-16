import { expect, test } from '@playwright/test'
import { database } from './helpers'

test('AC-06 组合写入后名额、成绩状态和故障触发器无残留不一致', () => {
  const checks: Record<string, string> = {
    人数不一致: `SELECT COUNT(*) FROM teaching_class tc LEFT JOIN
      (SELECT teaching_class_id, COUNT(*) n FROM enrollment WHERE deleted=0
       AND status IN ('ENROLLED','COMPLETED') GROUP BY teaching_class_id) e
      ON e.teaching_class_id=tc.id
      WHERE tc.deleted=0 AND tc.enrolled_count<>COALESCE(e.n,0)`,
    超出容量: 'SELECT COUNT(*) FROM teaching_class WHERE deleted=0 AND enrolled_count>capacity',
    已发布但未完成: `SELECT COUNT(*) FROM grade g JOIN enrollment e ON e.id=g.enrollment_id
      WHERE g.deleted=0 AND g.status='PUBLISHED' AND (e.deleted<>0 OR e.status<>'COMPLETED'
      OR e.teaching_class_id<>g.teaching_class_id)`,
    已完成但无发布成绩: `SELECT COUNT(*) FROM enrollment e LEFT JOIN grade g
      ON g.enrollment_id=e.id AND g.deleted=0 AND g.status='PUBLISHED'
      WHERE e.deleted=0 AND e.status='COMPLETED' AND g.id IS NULL`,
    故障触发器残留: 'SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()',
  }
  for (const [label, query] of Object.entries(checks)) expect(database(query), label).toBe('0')
})
