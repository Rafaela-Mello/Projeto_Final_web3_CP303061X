const express = require('express');
const path = require('path');
const axios = require('axios');

const app = express();
const PORT = 3000;
const USER_SERVICE_URL = process.env.USER_SERVICE_URL || 'http://127.0.0.1:8081';

app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.post('/send-code', async (req, res) => {
    const email = req.body.email?.trim();

    if (!email) {
        return res.status(400).send('Informe um e-mail válido.');
    }

    try {
        await axios.post(
            `${USER_SERVICE_URL}/auth/request-code`,
            { email },
            { headers: { 'Content-Type': 'application/json' }, timeout: 30000 }
        );
        res.redirect(`/verify?email=${encodeURIComponent(email)}`);
    } catch (error) {
        console.error('Erro em /send-code:', error.message, error.response?.status, error.response?.data);
        const status = error.response?.status;
        const message = error.response?.data?.message
            || (error.code === 'ECONNREFUSED'
                ? 'User Service indisponível. Verifique se o ms_user está rodando na porta 8081.'
                : status === 403
                    ? 'Acesso negado pelo User Service. Reinicie o ms_user após atualizar as configurações.'
                    : 'Erro ao solicitar código. Tente novamente.');
        res.status(status || 500).send(message);
    }
});

app.get('/verify', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'verify.html'));
});

app.post('/verify-code', async (req, res) => {
    const email = req.body.email?.trim();
    const code = req.body.code?.trim();

    if (!email || !code) {
        return res.status(400).send(renderErrorPage('Informe e-mail e código.'));
    }

    try {
        const response = await axios.post(`${USER_SERVICE_URL}/auth/verify-code`, { email, code });
        const token = response.data.token;

        res.send(`
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
                <meta charset="UTF-8">
                <title>Autenticando...</title>
            </head>
            <body>
                <p>Autenticando...</p>
                <script>
                    sessionStorage.setItem('token', ${JSON.stringify(token)});
                    window.location.href = '/register';
                </script>
            </body>
            </html>
        `);
    } catch (error) {
        const message = error.response?.data?.message || 'Código inválido ou expirado.';
        res.status(400).send(renderErrorPage(message, email));
    }
});

app.get('/dashboard', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'dashboard.html'));
});

app.get('/register', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'register.html'));
});

app.post('/register', async (req, res) => {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
        return res.status(401).send('Token não informado.');
    }

    const { name, role } = req.body;

    if (!name?.trim() || !role) {
        return res.status(400).send('Informe nome e cargo.');
    }

    try {
        await axios.post(
            `${USER_SERVICE_URL}/users/update-profile`,
            { name: name.trim(), role },
            {
                headers: {
                    Authorization: authHeader,
                    'Content-Type': 'application/json'
                },
                timeout: 30000
            }
        );
        res.status(200).json({ success: true });
    } catch (error) {
        console.error('Erro em /register:', error.message, error.response?.status, error.response?.data);
        const status = error.response?.status || 500;
        const message = error.response?.data?.message || 'Erro ao atualizar perfil.';
        res.status(status).send(message);
    }
});

app.get('/api/protected', async (req, res) => {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
        return res.status(401).send('Token não informado.');
    }

    try {
        const response = await axios.get(`${USER_SERVICE_URL}/users/test/customer`, {
            headers: { Authorization: authHeader },
            timeout: 30000
        });
        res.status(response.status).send(response.data);
    } catch (error) {
        const status = error.response?.status || 500;
        const message = error.response?.data || error.message;
        res.status(status).send(typeof message === 'string' ? message : JSON.stringify(message));
    }
});

app.get('/api/me', async (req, res) => {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
        return res.status(401).send('Token não informado.');
    }

    try {
        const response = await axios.get(`${USER_SERVICE_URL}/users/me`, {
            headers: { Authorization: authHeader },
            timeout: 30000
        });
        res.status(response.status).json(response.data);
    } catch (error) {
        const status = error.response?.status || 500;
        res.status(status).json(error.response?.data || { message: error.message });
    }
});

function renderErrorPage(message, email = '') {
    const emailParam = email ? `?email=${encodeURIComponent(email)}` : '';

    return `
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
            <meta charset="UTF-8">
            <title>Erro na verificação</title>
            <link rel="stylesheet" href="/styles.css">
        </head>
        <body>
            <main class="container">
                <h1>Erro na verificação</h1>
                <p class="error">${message}</p>
                <a href="/verify${emailParam}">Tentar novamente</a>
            </main>
        </body>
        </html>
    `;
}

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Frontend rodando em http://localhost:${PORT}`);
    console.log(`User Service: ${USER_SERVICE_URL}`);
});
