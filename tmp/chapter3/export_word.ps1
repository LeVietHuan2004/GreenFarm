param([string]$InputDocx,[string]$OutputPdf)
$ErrorActionPreference = 'Stop'
$wordApp = New-Object -ComObject Word.Application
$wordApp.Visible = $false
$wordApp.DisplayAlerts = 0
$wordDoc = $null
try {
  $wordDoc = $wordApp.Documents.Open($InputDocx,$false,$true,$false)
  $wordDoc.Repaginate()
  $wordDoc.ExportAsFixedFormat($OutputPdf,17)
  Write-Output "Exported $OutputPdf"
} finally {
  if ($null -ne $wordDoc) {$wordDoc.Close(0)}
  $wordApp.Quit()
  [void][Runtime.InteropServices.Marshal]::ReleaseComObject($wordApp)
}
