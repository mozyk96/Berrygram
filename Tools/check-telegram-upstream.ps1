[CmdletBinding()]
param([string]$MetadataPath = (Join-Path $PSScriptRoot 'telegram-upstream.json'))

$ErrorActionPreference = 'Stop'
try {
    $current = Get-Content -Raw -LiteralPath $MetadataPath | ConvertFrom-Json
    if ($current.integratedCommit -notmatch '^[0-9a-f]{40}$') { throw 'Invalid integrated upstream commit in metadata' }
    $headers = @{ 'User-Agent' = 'Berrygram-upstream-check'; 'Accept' = 'application/vnd.github+json' }
    $latest = Invoke-RestMethod -Headers $headers -Uri 'https://api.github.com/repos/DrKLO/Telegram/commits/master'
    $sha = [string]$latest.sha
    if ($sha -notmatch '^[0-9a-f]{40}$') { throw 'Official source commit is unavailable' }
    $properties = Invoke-RestMethod -Headers $headers -Uri "https://raw.githubusercontent.com/DrKLO/Telegram/$sha/gradle.properties"
    $versionMatch = [regex]::Match([string]$properties, '(?m)^APP_VERSION_NAME\s*=\s*([^\r\n]+)')
    $codeMatch = [regex]::Match([string]$properties, '(?m)^APP_VERSION_CODE\s*=\s*(\d+)')
    if (!$versionMatch.Success -or !$codeMatch.Success) { throw 'Official source version metadata is unavailable' }
    $status = 'unchanged'
    if ($sha -ne $current.integratedCommit) {
        $comparison = Invoke-RestMethod -Headers $headers -Uri "https://api.github.com/repos/DrKLO/Telegram/compare/$($current.integratedCommit)...$sha"
        $status = switch ($comparison.status) { 'ahead' { 'newer_source' } 'identical' { 'unchanged' } default { 'review_required' } }
        $version = $versionMatch.Groups[1].Value.Trim()
        if ([string]$latest.commit.message -notmatch ('(?i)^update to\s+' + [regex]::Escape($version) + '(\s|\(|$)')) { $status = 'review_required' }
    }
    [ordered]@{
        berrygramVersion = $current.berrygramVersion
        integratedVersion = $current.integratedVersion
        integratedCommit = $current.integratedCommit
        latestSourceVersion = $versionMatch.Groups[1].Value.Trim()
        latestSourceCode = [int]$codeMatch.Groups[1].Value
        latestSourceCommit = $sha
        status = $status
    } | ConvertTo-Json
} catch {
    [ordered]@{ status = 'unavailable'; error = $_.Exception.Message } | ConvertTo-Json
    exit 1
}
