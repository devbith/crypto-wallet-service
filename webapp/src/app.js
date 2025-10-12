const API_URL = 'http://localhost:8080/api/v1';
let currentWalletId = null;
let currentWallet = null;

document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    loadWalletList();
});

function setupEventListeners() {
    document.getElementById('showCreateWalletBtn').onclick = showCreateForm;
    document.getElementById('hideCreateWalletBtn').onclick = hideCreateForm;
    document.getElementById('createWalletForm').onsubmit = createWallet;
    document.getElementById('confirmAddAsset').onclick = addAsset;
    document.getElementById('runWalletSimulation').onclick = runSimulation;
    
    // Make asset symbols uppercase
    document.getElementById('assetSymbol').oninput = function() {
        this.value = this.value.toUpperCase();
    };
}

function showCreateForm() {
    document.getElementById('createWalletSection').style.display = 'block';
    document.getElementById('showCreateWalletBtn').style.display = 'none';
    document.getElementById('email').focus();
}

function hideCreateForm() {
    document.getElementById('createWalletSection').style.display = 'none';
    document.getElementById('showCreateWalletBtn').style.display = 'block';
    document.getElementById('createWalletForm').reset();
}

function showAddAssetForm() {
    document.getElementById('addAssetModal').style.display = 'block';
}

function hideAddAssetForm() {
    document.getElementById('addAssetModal').style.display = 'none';
    document.getElementById('addAssetForm').reset();
}

function formatMoney(amount) {
    return '$' + parseFloat(amount).toFixed(2);
}



function formatDate(dateString) {
    return new Date(dateString).toLocaleString();
}

async function createWallet(event) {
    event.preventDefault();
    const email = document.getElementById('email').value;
    const button = event.target.querySelector('button[type="submit"]');
    
    button.disabled = true;
    button.innerHTML = '';
    
    const response = await fetch(API_URL + '/wallets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    });


    const user = await response.json();
    currentWalletId = user.wallet_id;

    await loadWallet(user.wallet_id);

    hideCreateForm();
    button.disabled = false;
    button.innerHTML = 'Create Wallet';
}

async function loadWallet(walletId) {
    const response = await fetch(API_URL + '/wallets/' + walletId);
    const wallet = await response.json();
    currentWalletId = walletId;
    currentWallet = wallet;


    displayWallet(wallet);
}


function displayWallet(wallet) {
    document.getElementById('walletSection').style.display = 'block';
    document.getElementById('currentWalletId').textContent = wallet.id;
    document.getElementById('totalValue').textContent = formatMoney(wallet.total);
    document.getElementById('walletCreated').textContent = formatDate(wallet.created_at);
    
    const simButton = document.getElementById('runWalletSimulation');
    simButton.disabled = !wallet.assets || wallet.assets.length === 0;
    
    const assetsList = document.getElementById('assetsList');
    if (wallet.assets && wallet.assets.length > 0) {
        let html = '';
        for (let asset of wallet.assets) {
            html += '<div class="asset-item p-2 mb-2">' +
                '<strong>' + asset.symbol + '</strong> - ' +
                'Quantity: ' + asset.quantity + ' | ' +
                'Price: ' + formatMoney(asset.price) + ' | ' +
                'Value: <strong>' + formatMoney(asset.value) + '</strong>' +
            '</div>';
        }
        assetsList.innerHTML = html;
    } else {
        assetsList.innerHTML = '<p>No assets found</p>';
    }
}

async function addAsset() {
    const symbol = document.getElementById('assetSymbol').value;
    const quantity = document.getElementById('assetQuantity').value;
    const button = document.getElementById('confirmAddAsset');
    
    if (!currentWalletId) {
        return;
    }
    
    button.disabled = true;
    button.innerHTML = 'Adding...';
    
    const response = await fetch(API_URL + '/wallets/' + currentWalletId + '/assets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ symbol, quantity: parseFloat(quantity) })
    });
    
    const wallet = await response.json();
    displayWallet(wallet);
    
    document.getElementById('addAssetForm').reset();
    document.getElementById('addAssetModal').style.display = 'none';
    
    button.disabled = false;
    button.innerHTML = 'Add Asset';
}

async function loadWalletList() {
    const response = await fetch(API_URL + '/wallets');
    const wallets = await response.json();
    displayWalletList(wallets);
}

function displayWalletList(wallets) {
    const container = document.getElementById('walletsList');
    const count = document.getElementById('walletCount');
    
    count.textContent = wallets.length;
    
    if (wallets.length === 0) {
        container.innerHTML = '<p>No wallets found</p>';
        return;
    }
    
    let html = '';
    for (let wallet of wallets) {
        html += '<div class="border p-2 mb-2">' +
            '<strong>ID:</strong> ' + wallet.id + '<br>' +
            '<strong>Assets:</strong> ' + wallet.assets.length + ' | ' +
            '<strong>Total:</strong> ' + formatMoney(wallet.total) +
            '<button class="btn btn-primary btn-sm float-end" onclick="loadWallet(\'' + wallet.id + '\')">' +
                'Load' +
            '</button>' +
        '</div>';
    }
    container.innerHTML = html;
}

async function runSimulation() {
    const button = document.getElementById('runWalletSimulation');
    
    if (!currentWallet || !currentWallet.assets || currentWallet.assets.length === 0) {
        return;
    }
    
    button.disabled = true;
    button.innerHTML = 'Running...';
    
    const assets = currentWallet.assets.map(asset => ({
        symbol: asset.symbol,
        quantity: parseFloat(asset.quantity),
        value: parseFloat(asset.value)
    }));
    
    const response = await fetch(API_URL + '/profit-simulation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ assets })
    });
    
    const result = await response.json();
    displaySimulationResults(result);
    
    button.disabled = false;
    button.innerHTML = 'Simulate Profit/Loss';
}

function displaySimulationResults(result) {
    document.getElementById('walletSimulationResults').style.display = 'block';
    
    document.getElementById('walletSimTotalValue').textContent = formatMoney(result.total);
    
    if (result.best_asset) {
        document.getElementById('walletSimBestAsset').textContent = result.best_asset;
        document.getElementById('walletSimBestPerformance').textContent = '+' + result.best_performance.toFixed(2) + '%';
    }
    
    if (result.worst_asset) {
        document.getElementById('walletSimWorstAsset').textContent = result.worst_asset;
        const worstPerf = result.worst_performance;
        document.getElementById('walletSimWorstPerformance').textContent = 
            (worstPerf >= 0 ? '+' : '') + worstPerf.toFixed(2) + '%';
    }
}
