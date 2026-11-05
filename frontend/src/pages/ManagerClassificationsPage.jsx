import { Alert, Button, Card, Form, Input, InputNumber, Modal, Select, Spin, Table } from 'antd'
import { useEffect, useState } from 'react'
import ManagerReturnButton from '../components/ManagerReturnButton.jsx'
import { createCategory, deleteCategory, listCategories, updateCategory } from '../api/categories.js'
import { deleteCatalogue, getCatalogue, saveCatalogue } from '../api/discovery.js'
import '../ui/discovery.css'

const groups = [
  { key: 'categories', title: 'Renovation categories', sample: 'Kitchen renovation' },
  { key: 'styles', title: 'Design styles', sample: 'Japandi' },
  { key: 'property-types', title: 'Property types', sample: 'HDB' },
  { key: 'size-ranges', title: 'Floor area ranges', sample: 'Medium' },
]
export default function ManagerClassificationsPage() {
  const [kind, setKind] = useState('categories')
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(false)
  const [form] = Form.useForm()
  async function refresh(key = kind) {
    setLoading(true); setError('')
    try { setRows(key === 'categories' ? await listCategories() : await getCatalogue(key)) }
    catch { setError('Unable to load classifications. Please check your manager session.') }
    finally { setLoading(false) }
  }
  useEffect(() => { refresh(kind) }, [kind]) // eslint-disable-line react-hooks/exhaustive-deps
  function open(entry = {}) { setEditing(entry); form.setFieldsValue({ name: entry.name || '', minSqm: entry.min_sqm ?? null, maxSqm: entry.max_sqm ?? null }) }
  async function save(value) {
    setBusy(true); setError('')
    try {
      if (kind === 'categories') {
        if (editing?.id) await updateCategory(editing.id, { name: value.name })
        else await createCategory({ name: value.name })
      } else await saveCatalogue(kind, value, editing?.id)
      setEditing(null); await refresh()
    } catch (failure) { setError(failure.response?.data?.message || 'Unable to save. Check for duplicate names or overlapping size ranges.') }
    finally { setBusy(false) }
  }
  async function remove(entry) {
    setBusy(true); setError('')
    try { if (kind === 'categories') await deleteCategory(entry.id); else await deleteCatalogue(kind, entry.id); await refresh() }
    catch (failure) { setError(failure.response?.data?.message || 'Unable to delete classification.') }
    finally { setBusy(false) }
  }
  const range = kind === 'size-ranges'
  return <section className="discovery-page"><ManagerReturnButton /><p className="discovery-eyebrow">Platform management</p>
    <h1>Project classifications</h1><p>Manage renovation categories, design styles, property types and floor-area ranges. Deleting a classification never deletes projects.</p>
    {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
    <Card><div className="discovery-toolbar"><Select aria-label="Classification group" value={kind} style={{ minWidth: 250 }} onChange={setKind} options={groups.map(g => ({ value: g.key, label: g.title }))} />
      <Button type="primary" onClick={() => open()}>Add classification</Button></div>
      {loading ? <Spin /> : <Table rowKey="id" dataSource={rows} pagination={{ pageSize: 8 }} locale={{ emptyText: 'No classifications configured yet.' }}
        columns={[{ title: 'Name', dataIndex: 'name' }, ...(range ? [{ title: 'From (m²)', dataIndex: 'min_sqm', render: value => value ?? 'No lower bound' }, { title: 'To (m², exclusive)', dataIndex: 'max_sqm', render: value => value ?? 'No upper bound' }] : []),
          { title: 'Actions', render: (_, entry) => <div className="discovery-actions"><Button onClick={() => open(entry)}>Edit</Button>
            <Button danger disabled={busy} onClick={() => Modal.confirm({ title: 'Delete classification?', content: 'Projects and customers remain, but this classification will be removed from their selections.', okText: 'Delete', okButtonProps: { danger: true }, onOk: () => remove(entry) })}>Delete</Button></div> }]} />}
    </Card>
    <Modal title={editing?.id ? 'Edit classification' : 'Add classification'} open={editing !== null} onCancel={() => setEditing(null)} onOk={() => form.submit()} okButtonProps={{ loading: busy }}>
      <Form form={form} layout="vertical" onFinish={save}><Form.Item label="Name" name="name" rules={[{ required: true, whitespace: true }, { max: 100 }]}><Input maxLength={100} /></Form.Item>
        {range && <><Form.Item label="Minimum area (m², optional)" name="minSqm"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item label="Maximum area (m², exclusive; optional)" name="maxSqm"><InputNumber min={0.01} style={{ width: '100%' }} /></Form.Item></>}
      </Form>
    </Modal>
  </section>
}
