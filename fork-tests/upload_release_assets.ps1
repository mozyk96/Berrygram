[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Tag,
    [Parameter(Mandatory = $true)][string]$AssetList
)
$ErrorActionPreference = 'Stop'
$repo = 'mozyk96/Berrygram'
$Assets = $AssetList -split ';' | Where-Object { $_ }

# The token is read from the git credential store and is never printed.
$filled = @"
protocol=https
host=github.com

"@ | git credential fill
$token = ($filled | Where-Object { $_ -like 'password=*' }) -replace '^password=', ''
if (-not $token) { throw 'No GitHub token in the git credential store' }
$headers = @{
    'Authorization'  = "Bearer $token"
    'Accept'         = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent'     = 'berrygram-release'
}

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/tags/$Tag" -Headers $headers -Method Get
Write-Host "release $($release.id) $($release.html_url) prerelease=$($release.prerelease)"
$uploadBase = ($release.upload_url -split '\{')[0]

$existing = @()
foreach ($asset in (Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/$($release.id)/assets" -Headers $headers -Method Get)) {
    $existing += $asset.name
}

foreach ($path in $Assets) {
    $name = Split-Path -Leaf $path
    if ($existing -contains $name) {
        Write-Host "asset_present $name"
        continue
    }
    $uploadHeaders = @{
        'Authorization' = "Bearer $token"
        'Accept'        = 'application/vnd.github+json'
        'X-GitHub-Api-Version' = '2022-11-28'
        'User-Agent'    = 'berrygram-release'
        'Content-Type'  = 'application/octet-stream'
    }
    $uri = "${uploadBase}?name=$name"
    $result = Invoke-RestMethod -Uri $uri -Headers $uploadHeaders -Method Post -InFile $path -ContentType 'application/octet-stream'
    Write-Host "asset_uploaded $($result.name) $($result.size) $($result.state)"
}
