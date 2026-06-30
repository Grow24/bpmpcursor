<?php
/**
 * PBMP PHP Validation Backend
 * ---------------------------
 * This is PBMP's real PHP component that does the governance/validation work.
 * The Node workbench (server.js) calls THIS script when validating coding.
 * It runs real checks:
 *
 *   1. PHP syntax lint  -> `php -l` on every .php file
 *   2. Coding standards -> scan for forbidden patterns (var_dump/die/eval)
 *   3. Security scan     -> patterns like hardcoded password/secret
 *   4. Unit tests        -> run tests/run.sh if it exists in the project
 *
 * Usage:  php validate.php <projectPath> <language>
 * Output: JSON  { "passed": bool, "checks": [ { name, detail, pass } ] }
 */

error_reporting(E_ALL & ~E_DEPRECATED);

$projectPath = $argv[1] ?? '';
$language    = $argv[2] ?? '';

if ($projectPath === '' || !is_dir($projectPath)) {
    echo json_encode([
        'passed' => false,
        'checks' => [[
            'name'   => 'Project path',
            'detail' => "Project folder not found: {$projectPath}",
            'pass'   => false,
        ]],
        'engine' => 'php',
    ]);
    exit(0);
}

/** Recursively collect all files with a given extension */
function collectFiles(string $dir, string $ext): array {
    $out = [];
    $rii = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($dir, FilesystemIterator::SKIP_DOTS)
    );
    foreach ($rii as $file) {
        if ($file->isFile() && str_ends_with($file->getFilename(), $ext)) {
            // skip .git and node_modules
            $p = $file->getPathname();
            if (strpos($p, DIRECTORY_SEPARATOR . '.git') !== false) continue;
            $out[] = $p;
        }
    }
    return $out;
}

$checks = [];

/* ---------- CHECK 1: PHP syntax lint (php -l) ---------- */
$phpFiles = collectFiles($projectPath, '.php');
if (count($phpFiles) === 0) {
    $checks[] = [
        'name'   => 'PHP syntax lint',
        'detail' => 'No .php files found (skip).',
        'pass'   => true,
    ];
} else {
    $lintErrors = [];
    foreach ($phpFiles as $f) {
        $out = [];
        $code = 0;
        exec('php -l ' . escapeshellarg($f) . ' 2>&1', $out, $code);
        if ($code !== 0) {
            $lintErrors[] = basename($f) . ': ' . trim(implode(' ', $out));
        }
    }
    $checks[] = [
        'name'   => 'PHP syntax lint (php -l)',
        'detail' => count($lintErrors) === 0
            ? count($phpFiles) . ' PHP file(s) syntax OK'
            : implode(' | ', $lintErrors),
        'pass'   => count($lintErrors) === 0,
    ];
}

/* ---------- CHECK 2: Coding standards (forbidden patterns) ---------- */
$forbidden = ['var_dump(', 'die(', 'eval('];
$violations = [];
foreach ($phpFiles as $f) {
    $src = file_get_contents($f);
    foreach ($forbidden as $bad) {
        if (strpos($src, $bad) !== false) {
            $violations[] = basename($f) . " -> '{$bad}'";
        }
    }
}
$checks[] = [
    'name'   => 'Coding standards (PSR / no debug code)',
    'detail' => count($violations) === 0
        ? 'No forbidden patterns (var_dump/die/eval) found'
        : implode(' | ', $violations),
    'pass'   => count($violations) === 0,
];

/* ---------- CHECK 3: Security scan (hardcoded secrets) ---------- */
$secretPatterns = [
    '/password\s*=\s*[\'"][^\'"]+[\'"]/i',
    '/api[_-]?key\s*=\s*[\'"][^\'"]+[\'"]/i',
    '/secret\s*=\s*[\'"][^\'"]+[\'"]/i',
];
$secretHits = [];
foreach ($phpFiles as $f) {
    $src = file_get_contents($f);
    foreach ($secretPatterns as $pat) {
        if (preg_match($pat, $src)) {
            $secretHits[] = basename($f) . ' -> possible hardcoded secret';
            break;
        }
    }
}
$checks[] = [
    'name'   => 'Security scan (hardcoded secrets)',
    'detail' => count($secretHits) === 0
        ? 'No hardcoded secrets found'
        : implode(' | ', $secretHits),
    'pass'   => count($secretHits) === 0,
];

/* ---------- CHECK 4: Unit tests (if a test runner exists) ---------- */
$testRunner = $projectPath . DIRECTORY_SEPARATOR . 'tests' . DIRECTORY_SEPARATOR . 'run.sh';
if (is_file($testRunner)) {
    $out = [];
    $code = 0;
    exec('sh ' . escapeshellarg($testRunner) . ' 2>&1', $out, $code);
    $checks[] = [
        'name'   => 'Unit tests',
        'detail' => trim(implode(' ', $out)) ?: 'tests run',
        'pass'   => $code === 0,
    ];
} else {
    $checks[] = [
        'name'   => 'Unit tests',
        'detail' => 'No tests/run.sh found (skip -> pass)',
        'pass'   => true,
    ];
}

$passed = true;
foreach ($checks as $c) {
    if (!$c['pass']) { $passed = false; break; }
}

echo json_encode([
    'passed' => $passed,
    'checks' => $checks,
    'engine' => 'php',
    'language' => $language,
], JSON_UNESCAPED_SLASHES);
