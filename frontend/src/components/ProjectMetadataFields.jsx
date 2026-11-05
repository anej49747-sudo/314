import { Form, Input, Select } from 'antd'
import { useEffect, useState } from 'react'
import { getCatalogue } from '../api/discovery.js'
import { floorAreaValidationRule } from '../designer/floorAreaRules.js'
export default function ProjectMetadataFields({ form, initial = null }) {
  const [lists, setLists] = useState({ styles: [], 'property-types': [] })
  useEffect(() => { let alive = true; Promise.all([getCatalogue('styles'),getCatalogue('property-types')])
    .then(([styles, types]) => { if (alive) setLists({ styles, 'property-types': types }) }).catch(() => {})
    return () => { alive = false } }, [])
  useEffect(() => { if (initial) form.setFieldsValue({ propertyTypeId: initial.propertyTypeId || null, floorAreaSqm: initial.floorAreaSqm ?? null,
    primaryStyleId: initial.styles?.find(s => s.role === 'PRIMARY')?.id || null, secondaryStyleIds: initial.styles?.filter(s => s.role === 'SECONDARY').map(s => s.id) || [] }) }, [initial,form])
  const styles = lists.styles.map(s => ({ value:s.id, label:s.name }))
  const types = lists['property-types'].map(t => ({ value:t.id, label:t.name }))
  return <>
    <Form.Item name="primaryStyleId" label="Primary design style" rules={styles.length ? [{ required: true, message: 'Choose a primary style' }] : []}>
      <Select options={styles} disabled={!styles.length} placeholder={styles.length ? 'Select primary style' : 'Pending setup'} allowClear />
    </Form.Item>
    <Form.Item name="secondaryStyleIds" label="Other design styles (up to two)" rules={[{ validator: (_,v) => v?.length > 2 ? Promise.reject(new Error('Select no more than two secondary styles')) : Promise.resolve() }]}>
      <Select mode="multiple" maxCount={2} disabled={!styles.length} options={styles} placeholder={styles.length ? 'Optional' : 'Pending setup'} />
    </Form.Item>
    <Form.Item name="propertyTypeId" label="Property type" rules={types.length ? [{ required: true, message: 'Choose a property type' }] : []}>
      <Select options={types} disabled={!types.length} placeholder={types.length ? 'Select property type' : 'Pending setup'} allowClear />
    </Form.Item>
    <Form.Item name="floorAreaSqm" label="Floor area (m², optional)" rules={[floorAreaValidationRule()]} validateTrigger={['onChange','onBlur']}>
      <Input inputMode="decimal" maxLength={12} placeholder="e.g. 95.50" />
    </Form.Item>
  </>
}
