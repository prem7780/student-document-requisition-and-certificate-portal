import React, { useEffect, useState } from 'react'
import { api } from './api'
import { Empty, ErrorPanel } from './components'
import { CheckCircle2, ClipboardCheck, History, Plus, RefreshCw, Trash2 } from 'lucide-react'

const DEFAULT = ['SSC Original', 'Intermediate Original', 'Transfer Certificate', 'Original Marks Memo', 'Other Original Document']
const date = value => value ? new Date(value).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' }) : '—'

export default function OriginalCollection({ applicationId }) {
  const [data, setData] = useState(null)
  const [expected, setExpected] = useState(DEFAULT.map(name => ({ name, quantity: 1 })))
  const [addName, setAddName] = useState('')
  const [quantities, setQuantities] = useState({})
  const [identity, setIdentity] = useState(false)
  const [remarks, setRemarks] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  async function load() { setLoading(true); try { setData(await api(`/admin/original-collections/${applicationId}`)) } catch (err) { setError(err.message) } finally { setLoading(false) } }
  useEffect(() => { setData(null); setQuantities({}); load() }, [applicationId])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); await load(); setNotice('Collection details saved successfully.') }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const active = (data?.documents || []).filter(item => item.active)
  const remaining = active.reduce((sum, item) => sum + Math.max(0, item.expectedQuantity - item.collectedQuantity), 0)
  const collected = active.reduce((sum, item) => sum + item.collectedQuantity, 0)
  const total = active.reduce((sum, item) => sum + item.expectedQuantity, 0)
  const selectedItems = Object.entries(quantities).filter(([_, value]) => Number(value) > 0).map(([id, quantity]) => ({ documentId: Number(id), quantity: Number(quantity) }))

  return <section className="card campus-service-section p-5 sm:p-6" aria-label="Original certificate collection">
    <div className="mb-5 flex flex-wrap items-start justify-between gap-3"><div><p className="campus-eyebrow mb-1">Administration / No Dues Collection</p><h2 className="campus-service-heading flex items-center gap-2"><ClipboardCheck size={19} /> Original documents — physical handover</h2><p className="tiny mt-2">Create the expected-document list at the first visit. Record exactly what the student collects on each visit.</p></div><button type="button" className="btn btn-muted" onClick={load} disabled={loading}><RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh</button></div>
    <ErrorPanel error={error} />
    {notice && <p className="campus-service-note mb-5" role="status">{notice}</p>}
    {loading && !data && <p className="tiny py-3">Loading original-document records…</p>}

    {data && !data.initialized && <div>
      <div className="mb-4"><h3 className="campus-service-subheading">First visit — documents held by the college</h3><p className="tiny mt-1">Adjust this starting list to match the student's actual records.</p></div>
      <div className="space-y-3">{expected.map((item, index) => <div className="flex flex-wrap items-end gap-2" key={index}>
        <label className="min-w-[180px] flex-1"><span className="label">Document</span><input className="field" value={item.name} onChange={e => setExpected(old => old.map((x, i) => i === index ? { ...x, name: e.target.value } : x))} /></label>
        <label className="w-24"><span className="label">Quantity</span><input className="field" type="number" min={1} max={20} value={item.quantity} onChange={e => setExpected(old => old.map((x, i) => i === index ? { ...x, quantity: Number(e.target.value) } : x))} /></label>
        <button type="button" className="btn btn-muted" aria-label={`Remove ${item.name}`} onClick={() => setExpected(old => old.filter((_, i) => i !== index))}><Trash2 size={15} /> Remove</button>
      </div>)}</div>
      <div className="mt-5 flex flex-wrap gap-2"><button type="button" className="btn btn-muted" onClick={() => setExpected(old => [...old, { name: '', quantity: 1 }])}><Plus size={15} /> Add document</button><button type="button" className="btn btn-primary" disabled={busy || !expected.length || expected.some(x => !x.name.trim() || x.quantity < 1)} onClick={() => run(() => api(`/admin/original-collections/${applicationId}/initialize`, { method: 'POST', body: expected }))}>Confirm expected documents</button></div>
    </div>}

    {data?.initialized && <div>
      <div className="grid grid-cols-3 gap-3 mb-6">{[['Expected', total], ['Collected', collected], ['Remaining', remaining]].map(([name, value]) => <div className="campus-service-metric" key={name}><p className="tiny">{name}</p><strong>{value}</strong></div>)}</div>
      <h3 className="campus-service-subheading mb-3">Document collection status</h3>
      <div className="table-wrap"><table><thead><tr>{['Document', 'Expected', 'Collected', 'Remaining', 'Collect this visit', ''].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{(data.documents || []).map(item => {
        const outstanding = item.active ? Math.max(0, item.expectedQuantity - item.collectedQuantity) : 0
        return <tr key={item.id} className={!item.active ? 'opacity-60' : ''}><td className="font-medium">{item.documentName}{!item.active && <span className="tiny"> · Removed</span>}</td><td>{item.expectedQuantity}</td><td>{item.collectedQuantity}</td><td>{outstanding}</td><td>{outstanding > 0 ? <input className="field !w-24" type="number" min={0} max={outstanding} aria-label={`Quantity to collect: ${item.documentName}`} value={quantities[item.id] ?? 0} onChange={e => setQuantities(old => ({ ...old, [item.id]: Math.max(0, Math.min(outstanding, Number(e.target.value) || 0)) }))} /> : <span className="tiny">—</span>}</td><td>{item.active && item.collectedQuantity === 0 && <button type="button" className="btn btn-muted" disabled={busy} onClick={() => { const reason = window.prompt('Reason for removing this expected document:'); if (reason?.trim()) run(() => api(`/admin/original-collections/${applicationId}/documents/${item.id}/remove`, { method: 'POST', body: { reason: reason.trim() } })) }}>Remove</button>}</td></tr>
      })}</tbody></table></div>
      <div className="mt-4 flex flex-wrap gap-2"><input className="field max-w-sm flex-1" placeholder="Additional expected document name" aria-label="New expected document name" value={addName} onChange={e => setAddName(e.target.value)} /><button type="button" className="btn btn-muted" disabled={!addName.trim() || busy} onClick={() => run(async () => { await api(`/admin/original-collections/${applicationId}/documents`, { method: 'POST', body: { name: addName, quantity: 1 } }); setAddName('') })}><Plus size={15} /> Add expected document</button></div>
      {remaining > 0 ? <div className="mt-6 rounded-md border border-slate-200 bg-[#fafbfc] p-5">
        <h3 className="campus-service-subheading mb-3">Record this collection visit</h3>
        <label className="flex gap-3 text-sm leading-6 text-slate-700"><input type="checkbox" className="mt-1.5" checked={identity} onChange={e => setIdentity(e.target.checked)} /><span>I have verified the student in person and confirm that only the student is collecting these documents.</span></label>
        <label className="block mt-4"><span className="label">Handover remarks (optional)</span><textarea className="field" rows={2} value={remarks} onChange={e => setRemarks(e.target.value)} /></label>
        <div className="flex flex-wrap items-center justify-between gap-3 mt-5"><p className="tiny">{selectedItems.reduce((sum, item) => sum + item.quantity, 0)} document(s) selected for this visit</p><button className="btn btn-primary" disabled={busy || !identity || !selectedItems.length} onClick={() => run(async () => {
          await api(`/admin/original-collections/${applicationId}/visits`, { method: 'POST', body: { identityVerified: identity, remarks, items: selectedItems } })
          setQuantities({}); setIdentity(false); setRemarks('')
        })}> <CheckCircle2 size={16} /> Confirm physical handover</button></div>
      </div> : <p className="campus-service-note mt-6"><CheckCircle2 size={16} className="inline mr-2" />All expected documents have been collected.</p>}
      <div className="mt-7"><h3 className="campus-service-subheading flex items-center gap-2 mb-3"><History size={17} /> Collection visits</h3><div className="rounded-md border border-slate-200 px-4">{(data.visits || []).map(record => <div className="campus-service-row" key={record.visit.id}><p className="text-sm font-semibold">Visit #{record.visit.id} · {date(record.visit.handedOverAt)}</p><p className="tiny mt-1">{(record.items || []).map(line => { const d = data.documents.find(x => x.id === line.documentId); return `${d?.documentName || 'Document'} × ${line.quantity}` }).join(', ')} · {record.visit.remarks || 'No remarks'}</p></div>)}{!data.visits?.length && <Empty message="No handover visits recorded yet." />}</div></div>
      <div className="mt-7"><h3 className="campus-service-subheading mb-3">Checklist and handover audit</h3><div className="rounded-md border border-slate-200 px-4">{(data.history || []).map(event => <div key={event.id} className="campus-service-row"><p className="text-sm font-medium">{String(event.action).replaceAll('_', ' ')}</p><p className="tiny mt-1">{date(event.occurredAt)} · {event.details}</p></div>)}{!data.history?.length && <p className="tiny py-4">No audit events recorded.</p>}</div></div>
    </div>}
  </section>
}
