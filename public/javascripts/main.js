function triggerAuthentication(csrfToken) {
    const authenticatedButton = document.getElementById('authenticationButton');
    authenticatedButton.classList.remove('btn-primary', 'btn-danger', 'btn-success');
    authenticatedButton.classList.add('btn-outline-warning');
    authenticatedButton.innerHTML = "Authenticating...";
    return fetch('http://localhost:9000/taleo/authenticate', {
        method: 'POST',
        headers: {
            'Csrf-Token': csrfToken
        }
    }).then(response => {
        if (response.status === 200) {
            authenticatedButton.classList.remove('btn-primary', 'btn-danger', 'btn-outline-warning');
            authenticatedButton.classList.add('btn-success');
            authenticatedButton.innerHTML = "Authenticated"
        } else {
            authenticatedButton.classList.remove('btn-primary', 'btn-success', 'btn-outline-warning');
            authenticatedButton.classList.add('btn-danger');
            authenticatedButton.innerHTML = "Retry authentication"
        }
    }
    );
}

function triggerSync(csrfToken) {
    const syncButton = document.getElementById('syncButton');
    syncButton.classList.remove('btn-primary', 'btn-danger', 'btn-success');
    syncButton.classList.add('btn-outline-warning');
    syncButton.innerHTML = "Syncing...";
    return fetch('http://localhost:9000/taleo/sync', {
        method: 'POST',
        headers: {
            'Csrf-Token': csrfToken
        }
    }).then(response => {
            if (response.status === 200) {
                response.json().then(json => {
                    if (json.error) {
                        syncButton.classList.remove('btn-outline-warning', 'btn-success', 'btn-warning');
                        syncButton.classList.add('btn-danger');
                        syncButton.innerHTML = "Failed - retry sync"
                    } else {
                        syncButton.classList.remove('btn-outline-warning', 'btn-danger', 'btn-warning');
                        syncButton.classList.add('btn-success');
                        syncButton.innerHTML = `Synchronised - ${json.added} added`
                    }
                })
            } else {
                syncButton.classList.remove('btn-outline-warning', 'btn-success', 'btn-warning');
                syncButton.classList.add('btn-danger');
                syncButton.innerHTML = "Failed - retry sync"
            }
        }
    );
}