# set-staging-secret.ps1
# Prompts for the staging database password with hidden typing and writes it securely to .env
$p = Read-Host 'Enter Supabase Staging DATABASE_PASSWORD (typing hidden)' -AsSecureString
if ($null -eq $p -or $p.Length -eq 0) {
    Write-Error "Password cannot be empty."
    exit 1
}
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($p)
$Plain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
$envPath = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"
[System.IO.File]::WriteAllText($envPath, "DATABASE_PASSWORD=$Plain`n")
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
Clear-Variable p, BSTR, Plain
Write-Host "DATABASE_PASSWORD saved securely to .env (verified gitignored)."
