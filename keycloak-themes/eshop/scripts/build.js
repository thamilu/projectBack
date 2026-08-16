const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const root = path.join(__dirname, '..');

// 0. Copy design tokens
console.log('Copying design tokens...');
try {
  fs.copyFileSync(
    path.join(root, 'login/resources/css/abstracts/tokens.css'),
    path.join(root, 'login/resources/css/tokens.css')
  );
} catch (err) {
  console.error('Copying tokens failed:', err);
  process.exit(1);
}

// 1. Run PostCSS build
console.log('Compiling CSS via PostCSS...');
try {
  execSync('npx postcss login/resources/css/login.css -o login/resources/css/login.min.css', { cwd: root, stdio: 'inherit' });
} catch (err) {
  console.error('PostCSS compilation failed:', err);
  process.exit(1);
}

// 2. Run esbuild
console.log('Bundling JS via esbuild...');
try {
  execSync('npx esbuild login/resources/js/login.js --bundle --minify --outfile=login/resources/js/login.min.js', { cwd: root, stdio: 'inherit' });
} catch (err) {
  console.error('esbuild compilation failed:', err);
  process.exit(1);
}

// 3. Compute SRI Hashes
function getSRI(filePath) {
  const content = fs.readFileSync(filePath);
  const hash = crypto.createHash('sha384').update(content).digest('base64');
  return `sha384-${hash}`;
}

function getFilesRecursive(dir, ext) {
  let results = [];
  if (!fs.existsSync(dir)) return results;
  const list = fs.readdirSync(dir);
  list.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    if (stat && stat.isDirectory()) {
      results = results.concat(getFilesRecursive(filePath, ext));
    } else if (filePath.endsWith(ext)) {
      results.push(filePath);
    }
  });
  return results;
}

const cssPath = path.join(root, 'login/resources/css/login.min.css');
const jsPath = path.join(root, 'login/resources/js/login.min.js');

const cssHash = getSRI(cssPath);
const jsHash = getSRI(jsPath);

console.log(`CSS SRI Hash: ${cssHash}`);
console.log(`JS SRI Hash:  ${jsHash}`);

// Generate SRI hashes for ALL CSS and JS files in resources
console.log('Generating SRI hashes for all resources...');
const resourcesDir = path.join(root, 'login/resources');
const cssFiles = getFilesRecursive(path.join(resourcesDir, 'css'), '.css');
const jsFiles = getFilesRecursive(path.join(resourcesDir, 'js'), '.js');
const allFiles = [...cssFiles, ...jsFiles];

let sriProperties = '';
allFiles.forEach(file => {
  const relativePath = path.relative(resourcesDir, file).replace(/\\/g, '/');
  const key = 'hash_' + relativePath.replace(/\//g, '_').replace(/\./g, '_');
  const hash = getSRI(file);
  sriProperties += `${key}=${hash}\n`;
});

// 4. Update theme.properties
const propertiesPath = path.join(root, 'login/theme.properties');
let properties = fs.readFileSync(propertiesPath, 'utf8');

// Sets `key=value` whether or not the line already exists — a plain regex .replace()
// silently does nothing when the target line is absent (this previously left `scripts=`
// missing from theme.properties entirely, since it never existed in the file for the
// regex to match: PostCSS/esbuild output was correctly built, but Keycloak had no
// reference to login.min.js at all, so none of this theme's client-side JS ever loaded).
function setProperty(text, key, value) {
  const pattern = new RegExp(`^${key}=.*$`, 'm');
  if (pattern.test(text)) {
    return text.replace(pattern, `${key}=${value}`);
  }
  return text.trim() + `\n${key}=${value}`;
}

properties = setProperty(properties, 'loginMinCssHash', cssHash);
properties = setProperty(properties, 'loginMinJsHash', jsHash);

// Strip out-of-date individual development script/style lists to favor minified bundle
properties = setProperty(properties, 'styles', 'css/tokens.css css/login.min.css');
properties = setProperty(properties, 'scripts', 'js/login.min.js');

// Remove all existing hash_* properties and dynamic comment headers
let lines = properties.split(/\r?\n/);
lines = lines.filter(line => !line.startsWith('hash_') && !line.includes('Dynamic SRI Hashes'));
properties = lines.join('\n');

// Append new hash properties
properties = properties.trim() + '\n\n# ─── Dynamic SRI Hashes (auto-generated) ────────────────\n' + sriProperties;


fs.writeFileSync(propertiesPath, properties, 'utf8');
console.log('Successfully updated theme.properties with CSS/JS references and dynamic SRI hashes.');
