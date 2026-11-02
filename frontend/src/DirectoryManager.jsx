import React, { useEffect, useState } from 'react'
import { api } from './api'
import { Empty, ErrorPanel } from './components'
import { Download, FileSpreadsheet, RefreshCw, Search, Upload } from 'lucide-react'

export default function DirectoryManager() {
  const [records, setRecords] = useState([])
  const [imports, setImports] = useState([])
  const [query, setQuery] = useState('')
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)

  async function load() {
    setLoading(true)
    try {
      const [students, batches] = await Promise.all([
        api(`/admin/directory?q=${encodeURIComponent(query)}`),
        api('/admin/directory/imports'),
      ])
      setRecords(Array.isArray(students) ? students : [])
      setImports(Array.isArray(batches) ? batches : [])
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [query])

  async function download(path, name) {
    const response = await fetch(`/api${path}`, { credentials: 'same-origin' })
    if (!response.ok) throw new Error('Unable to download this file.')
    const blob = await response.blob(), url = URL.createObjectURL(blob), link = document.createElement('a')
    link.href = url; link.download = name; document.body.appendChild(link); link.click(); link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1200)
  }
  async function upload(event) {
    event.preventDefault()
    if (!file) return
    setBusy(true); setError(''); setSummary(null)
    try {
      // Multipart upload must not set a JSON Content-Type; browser supplies boundary.
      const token = (await api('/public/csrf')).token
      const body = new FormData(); body.append('file', file)
      const response = await fetch('/api/admin/directory/upload', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'X-XSRF-TOKEN': token, 'X-User-Activity': '1' }, body,
      })
      if (!response.ok) { const details = await response.json().catch(() => ({})); throw new Error(details.detail || details.message || 'Student Directory import failed.') }
      setSummary(await response.json())
      await load()
    } catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }

  return <section className="campus-service-section space-y-5" aria-label="Student Directory">
    <div className="card p-5 sm:p-6">
      <header className="mb-5 flex flex-wrap items-start justify-between gap-3"><div><p className="campus-eyebrow mb-1">Administration / Student Directory</p><h2 className="campus-service-heading flex items-center gap-2"><FileSpreadsheet size={19} /> Import student details</h2><p className="tiny mt-2">CSV or XLSX (maximum 5 MB). Required: roll number, student name, branch and email. Batch / Year is optional.</p></div><button type="button" className="btn btn-muted" onClick={() => load()} disabled={loading}><RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh</button></header>
      <ErrorPanel error={error} />
      <form onSubmit={upload} className="flex flex-wrap items-end gap-3">
        <label className="min-w-0 flex-1 sm:max-w-sm"><span className="label">Select a CSV or Excel file</span><input type="file" className="field !py-2" accept=".csv,.xlsx" required onChange={e => setFile(e.target.files?.[0] || null)} /></label>
        <button type="submit" className="btn btn-primary" disabled={busy || !file}><Upload size={15} />{busy ? 'Importing…' : 'Upload student data'}</button>
        <button type="button" className="btn btn-muted" onClick={() => download('/admin/directory/template', 'student-directory-template.csv').catch(err => setError(err.message))}><Download size={15} /> CSV template</button>
      </form>
      {summary && <div className="campus-service-note mt-5" role="status">
        <h3 className="campus-service-subheading mb-3">Import results</h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[['New', summary.created], ['Updated', summary.updated], ['Unchanged', summary.unchanged], ['Skipped', summary.invalid]].map(([name, count]) => <div className="campus-service-metric" key={name}><p className="tiny">{name}</p><strong>{Number(count || 0)}</strong></div>)}
        </div>
        {summary.invalid > 0 && <button type="button" className="btn btn-muted mt-4" onClick={() => download(`/admin/directory/imports/${summary.importId}/errors`, 'student-import-errors.csv').catch(err => setError(err.message))}><Download size={15} /> Download rejected rows</button>}
      </div>}
    </div>
    <div className="card p-5 sm:p-6">
      <div className="flex flex-wrap items-start justify-between gap-4 mb-5"><div><h3 className="campus-service-subheading">Student Directory</h3><p className="tiny mt-1">Search imported student records by roll number or name.</p></div><label className="relative block min-w-[220px]"><span className="sr-only">Search directory</span><Search size={15} className="absolute left-3 top-3.5 text-slate-400" /><input className="field !pl-10" value={query} placeholder="Search students" onChange={e => setQuery(e.target.value)} /></label></div>
      <div className="table-wrap"><table><thead><tr>{['Roll number', 'Student name', 'Official branch', 'Mapped email', 'Batch'].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{records.map(student => <tr key={student.id}><td className="font-semibold">{student.rollNumber}</td><td>{student.studentName}</td><td>{student.branch}</td><td className="break-all">{student.email}</td><td>{student.batchYear || '—'}</td></tr>)}</tbody></table></div>
      {!records.length && !loading && <Empty message="No matching directory records." />}
    </div>
    <div className="card p-5 sm:p-6"><h3 className="campus-service-subheading mb-4">Recent imports</h3>
      {imports.map(batch => <div key={batch.id} className="campus-service-row flex flex-wrap items-center justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{batch.fileName}</p><p className="tiny mt-1">{batch.createdAt} · {batch.totalRows} rows · {batch.createdRows} new · {batch.updatedRows} updated · {batch.invalidRows} skipped</p></div>{batch.invalidRows > 0 && <button type="button" className="btn btn-muted" onClick={() => download(`/admin/directory/imports/${batch.id}/errors`, `student-import-${batch.id}-errors.csv`).catch(err => setError(err.message))}><Download size={15} /> Errors CSV</button>}</div>)}
      {!imports.length && <p className="tiny">No import history yet.</p>}
    </div>
  </section>
}
