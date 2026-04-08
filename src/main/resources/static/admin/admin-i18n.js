(function () {
    'use strict';

    var STORAGE_KEY = 'dusterAdminLang';

    var STRINGS = {
        ru: {
            common: {
                language: 'Язык',
                loggedInAs: 'Вы вошли как {id} (MAN)',
                navAria: 'Разделы админки',
                logout: 'Выйти',
                yes: 'да',
                errorStatus: 'Ошибка: {status}'
            },
            nav: {
                clients: 'Клиенты и устройства',
                messages: 'Сообщения',
                saved: 'Сохраняемые'
            },
            login: {
                documentTitle: 'Вход — админ',
                heading: 'Вход в админку',
                introHtml: 'Доступ к админке (клиенты и сообщения) только для роли <strong>MAN</strong>. Тот же токен используется для REST.',
                deviseId: 'deviseId',
                deviseIdPlaceholder: 'логин клиента',
                password: 'Пароль',
                signIn: 'Войти',
                errForbidden: 'Веб-интерфейс доступен только пользователям с ролью MAN.',
                errManOnlyWeb: 'Вход в веб-интерфейс разрешён только роли MAN. Для REST используйте POST /auth/login и заголовок Authorization.'
            },
            clients: {
                documentTitle: 'Админ — клиенты и устройства',
                header: 'Клиенты и устройства',
                subtitle: 'CRUD для сущности <code>Client</code> — REST: <code>/admin/api/clients</code> (JWT, роль MAN)',
                formNew: 'Новый клиент',
                formEdit: 'Редактирование {id}',
                deviseIdLabel: 'deviseId (уникальный)',
                deviseIdPh: 'например device-001',
                passwordLabel: 'Пароль',
                passwordPh: 'при создании обязателен; при правке — пусто = не менять',
                role: 'Роль',
                description: 'Описание',
                descriptionPh: 'что за устройство / пользователь',
                create: 'Создать',
                save: 'Сохранить',
                resetForm: 'Сбросить форму',
                list: 'Список',
                colId: 'ID',
                colDeviseId: 'deviseId',
                colPassword: 'Пароль',
                colRole: 'Роль',
                colDescription: 'Описание',
                colActions: '',
                empty: 'Записей нет.',
                flashLoadErr: 'Ошибка загрузки: {status}',
                edit: 'Изменить',
                del: 'Удалить',
                confirmDelete: 'Удалить клиента {id}?',
                flashNeedPassword: 'Укажите пароль для нового клиента',
                flashSaved: 'Сохранено',
                flashCreated: 'Создано',
                flashDeleteFail: 'Удаление не удалось: {status}',
                flashDeleted: 'Удалено'
            },
            messages: {
                documentTitle: 'Админ — сообщения',
                header: 'Сообщения',
                subtitle: 'Отправка на устройства (роль DEVISE) и просмотр истории. REST: <code>PUT /producer/request/…</code>, <code>GET /admin/api/messages</code>',
                sendTitle: 'Отправка сообщения на устройство',
                sendHint: 'Устройства задаются на странице «Клиенты и устройства» (клиент с ролью <code>DEVISE</code>). Статус после отправки: <code>GET /producer/getMessageStatus/{id}</code>',
                device: 'Устройство (роль DEVISE)',
                choose: '— выберите —',
                guarantee: 'Гарантия доставки',
                command: 'Команда (command)',
                commandPh: 'например digitalWrite',
                data: 'Данные (JSON-объект, необязательно)',
                dataPh: '{ "pinNumber": 13, "pinValue": true }',
                send: 'Отправить',
                stopPoll: 'Остановить опрос статуса',
                sentTitle: 'Отправленные сообщения',
                sentHint: '<code>GET /admin/api/messages</code> — последние записи, новые сверху. <strong>Клик по строке</strong> подставляет устройство, гарантию, команду и данные в форму отправки выше.',
                filterDevice: 'Фильтр по устройству',
                allDevices: 'Все устройства',
                limit: 'Сколько записей',
                refresh: 'Обновить список',
                colId: 'ID',
                colDevice: 'Устройство',
                colCommand: 'Команда',
                colStatus: 'Статус',
                colGuarantee: 'Гарантия',
                colCreated: 'Создано',
                colDelivered: 'Доставлено',
                colError: 'Ошибка',
                colData: 'Данные',
                empty: 'Сообщений нет (или нет подходящих под фильтр).',
                flashClientsErr: 'Не удалось загрузить список клиентов: {status}',
                noDevise: 'Нет устройств — создайте клиента с ролью DEVISE',
                fromHistory: ' (из истории, нет среди клиентов DEVISE)',
                flashMessagesErr: 'Не удалось загрузить сообщения: {status}',
                flashFilledFrom: 'Поля отправки заполнены из сообщения #{id}',
                ariaApplyRow: 'Подставить в форму отправки сообщение {id}',
                msgId: 'ID сообщения:',
                deliveryStatus: 'Статус доставки:',
                pollFinal: 'Финальный статус (опрос не требуется).',
                pollRunning: 'Идёт опрос статуса (до {seconds} с)…',
                pollTimeout: 'Опрос остановлен по таймауту. Последний показанный статус актуален на момент последнего запроса.',
                pollStopped: 'Опрос остановлен вручную.',
                pollDone: 'Достигнут финальный статус.',
                flashNoDevisePage: 'Нет устройств с ролью DEVISE. Добавьте их на странице «Клиенты и устройства».',
                flashPickDevice: 'Выберите устройство',
                flashDataObject: 'Поле «Данные» должно быть JSON-объектом {...}',
                flashBadJson: 'Некорректный JSON в поле «Данные»',
                flashCreated: 'Сообщение создано, id={id}'
            },
            saved: {
                documentTitle: 'Админ — сохраняемые сообщения',
                header: 'Сохраняемые сообщения',
                subtitle: 'Шаблоны команд для устройств (DEVISE): CRUD и отправка только на устройство-владелец. REST: <code>/admin/api/saved-messages</code>',
                newTpl: 'Новый шаблон',
                newTplHint: 'Шаблон привязывается к устройству (клиент с ролью <code>DEVISE</code>). Кнопка «Отправить» шлёт команду только этому владельцу.',
                owner: 'Устройство-владелец шаблона',
                guarantee: 'Гарантия доставки',
                description: 'Описание',
                descriptionPh: 'что делает команда',
                command: 'Команда (command)',
                commandPh: 'например digitalWrite',
                data: 'Данные (JSON-объект, необязательно)',
                dataPh: '{ "pinNumber": 13, "pinValue": true }',
                createTpl: 'Создать шаблон',
                afterSendTitle: 'Статус после отправки',
                afterSendHint: 'В таблице «Отправить» доставляет шаблон только на <strong>устройство-владелец</strong> строки (как <code>PUT /producer/request/{deviseId}</code> для его <code>deviseId</code>).',
                stopPoll: 'Остановить опрос статуса',
                listTitle: 'Список шаблонов',
                filterOwner: 'Фильтр по устройству-владельцу',
                allTpls: 'Все шаблоны',
                refresh: 'Обновить',
                colId: 'ID',
                colOwner: 'Владелец',
                colDescription: 'Описание',
                colCommand: 'Команда',
                colGuarantee: 'Гарантия',
                colData: 'Данные',
                colActions: '',
                empty: 'Шаблонов нет (или нет под фильтр).',
                noDevise: 'Нет устройств DEVISE',
                flashClientsErr: 'Не удалось загрузить клиентов: {status}',
                flashListErr: 'Ошибка загрузки списка: {status}',
                pickOwner: 'Выберите устройство-владелец',
                flashDataObject: 'Поле «Данные» должно быть JSON-объектом {...}',
                flashBadJson: 'Некорректный JSON в поле «Данные»',
                flashTplCreated: 'Шаблон создан',
                confirmDelete: 'Удалить шаблон {id}?',
                flashDeleteFail: 'Удаление не удалось: {status}',
                flashDeleted: 'Удалено',
                send: 'Отправить',
                del: 'Удалить',
                msgId: 'ID сообщения:',
                deliveryStatus: 'Статус доставки:',
                pollFinal: 'Финальный статус (опрос не требуется).',
                pollRunning: 'Идёт опрос статуса (до {seconds} с)…',
                pollTimeout: 'Опрос остановлен по таймауту.',
                pollDone: 'Достигнут финальный статус.',
                pollStopped: 'Опрос остановлен вручную.',
                flashSent: 'Сообщение отправлено из шаблона, id={id}'
            }
        },
        en: {
            common: {
                language: 'Language',
                loggedInAs: 'Signed in as {id} (MAN)',
                navAria: 'Admin sections',
                logout: 'Sign out',
                yes: 'yes',
                errorStatus: 'Error: {status}'
            },
            nav: {
                clients: 'Clients & devices',
                messages: 'Messages',
                saved: 'Saved templates'
            },
            login: {
                documentTitle: 'Sign in — admin',
                heading: 'Admin sign-in',
                introHtml: 'The admin UI (clients and messages) is only for the <strong>MAN</strong> role. The same token is used for REST.',
                deviseId: 'deviseId',
                deviseIdPlaceholder: 'client login',
                password: 'Password',
                signIn: 'Sign in',
                errForbidden: 'The web UI is only available to users with the MAN role.',
                errManOnlyWeb: 'The web UI allows only the MAN role. For REST use POST /auth/login and the Authorization header.'
            },
            clients: {
                documentTitle: 'Admin — clients & devices',
                header: 'Clients & devices',
                subtitle: 'CRUD for <code>Client</code> — REST: <code>/admin/api/clients</code> (JWT, MAN role)',
                formNew: 'New client',
                formEdit: 'Edit {id}',
                deviseIdLabel: 'deviseId (unique)',
                deviseIdPh: 'e.g. device-001',
                passwordLabel: 'Password',
                passwordPh: 'required when creating; when editing — leave empty to keep unchanged',
                role: 'Role',
                description: 'Description',
                descriptionPh: 'device or user description',
                create: 'Create',
                save: 'Save',
                resetForm: 'Reset form',
                list: 'List',
                colId: 'ID',
                colDeviseId: 'deviseId',
                colPassword: 'Password',
                colRole: 'Role',
                colDescription: 'Description',
                colActions: '',
                empty: 'No records.',
                flashLoadErr: 'Load failed: {status}',
                edit: 'Edit',
                del: 'Delete',
                confirmDelete: 'Delete client {id}?',
                flashNeedPassword: 'Enter a password for the new client',
                flashSaved: 'Saved',
                flashCreated: 'Created',
                flashDeleteFail: 'Delete failed: {status}',
                flashDeleted: 'Deleted'
            },
            messages: {
                documentTitle: 'Admin — messages',
                header: 'Messages',
                subtitle: 'Send to devices (DEVISE role) and browse history. REST: <code>PUT /producer/request/…</code>, <code>GET /admin/api/messages</code>',
                sendTitle: 'Send message to device',
                sendHint: 'Devices are managed under <strong>Clients & devices</strong> (client with <code>DEVISE</code> role). After send, status: <code>GET /producer/getMessageStatus/{id}</code>',
                device: 'Device (DEVISE role)',
                choose: '— select —',
                guarantee: 'Delivery guarantee',
                command: 'Command',
                commandPh: 'e.g. digitalWrite',
                data: 'Data (JSON object, optional)',
                dataPh: '{ "pinNumber": 13, "pinValue": true }',
                send: 'Send',
                stopPoll: 'Stop status polling',
                sentTitle: 'Sent messages',
                sentHint: '<code>GET /admin/api/messages</code> — latest first. <strong>Click a row</strong> to fill the send form above with device, guarantee, command and data.',
                filterDevice: 'Filter by device',
                allDevices: 'All devices',
                limit: 'Row limit',
                refresh: 'Refresh list',
                colId: 'ID',
                colDevice: 'Device',
                colCommand: 'Command',
                colStatus: 'Status',
                colGuarantee: 'Guarantee',
                colCreated: 'Created',
                colDelivered: 'Delivered',
                colError: 'Error',
                colData: 'Data',
                empty: 'No messages (or none match the filter).',
                flashClientsErr: 'Could not load clients: {status}',
                noDevise: 'No devices — create a client with DEVISE role',
                fromHistory: ' (from history, not in DEVISE clients)',
                flashMessagesErr: 'Could not load messages: {status}',
                flashFilledFrom: 'Send form filled from message #{id}',
                ariaApplyRow: 'Apply message {id} to send form',
                msgId: 'Message ID:',
                deliveryStatus: 'Delivery status:',
                pollFinal: 'Terminal status (no polling needed).',
                pollRunning: 'Polling status (up to {seconds} s)…',
                pollTimeout: 'Polling stopped on timeout. Last status is from the last successful request.',
                pollStopped: 'Polling stopped manually.',
                pollDone: 'Terminal status reached.',
                flashNoDevisePage: 'No DEVISE devices. Add them under Clients & devices.',
                flashPickDevice: 'Select a device',
                flashDataObject: 'Data field must be a JSON object {...}',
                flashBadJson: 'Invalid JSON in Data field',
                flashCreated: 'Message created, id={id}'
            },
            saved: {
                documentTitle: 'Admin — saved messages',
                header: 'Saved message templates',
                subtitle: 'Command templates for DEVISE devices: CRUD and send only to the owning device. REST: <code>/admin/api/saved-messages</code>',
                newTpl: 'New template',
                newTplHint: 'A template is bound to a device (client with <code>DEVISE</code> role). <strong>Send</strong> delivers only to that owner.',
                owner: 'Template owner device',
                guarantee: 'Delivery guarantee',
                description: 'Description',
                descriptionPh: 'what the command does',
                command: 'Command',
                commandPh: 'e.g. digitalWrite',
                data: 'Data (JSON object, optional)',
                dataPh: '{ "pinNumber": 13, "pinValue": true }',
                createTpl: 'Create template',
                afterSendTitle: 'Status after send',
                afterSendHint: 'The <strong>Send</strong> button in the table delivers the template only to the <strong>row owner device</strong> (same as <code>PUT /producer/request/{deviseId}</code> for its <code>deviseId</code>).',
                stopPoll: 'Stop status polling',
                listTitle: 'Template list',
                filterOwner: 'Filter by owner device',
                allTpls: 'All templates',
                refresh: 'Refresh',
                colId: 'ID',
                colOwner: 'Owner',
                colDescription: 'Description',
                colCommand: 'Command',
                colGuarantee: 'Guarantee',
                colData: 'Data',
                colActions: '',
                empty: 'No templates (or none match the filter).',
                noDevise: 'No DEVISE devices',
                flashClientsErr: 'Could not load clients: {status}',
                flashListErr: 'Could not load list: {status}',
                pickOwner: 'Select owner device',
                flashDataObject: 'Data field must be a JSON object {...}',
                flashBadJson: 'Invalid JSON in Data field',
                flashTplCreated: 'Template created',
                confirmDelete: 'Delete template {id}?',
                flashDeleteFail: 'Delete failed: {status}',
                flashDeleted: 'Deleted',
                send: 'Send',
                del: 'Delete',
                msgId: 'Message ID:',
                deliveryStatus: 'Delivery status:',
                pollFinal: 'Terminal status (no polling needed).',
                pollRunning: 'Polling status (up to {seconds} s)…',
                pollTimeout: 'Polling stopped on timeout.',
                pollDone: 'Terminal status reached.',
                pollStopped: 'Polling stopped manually.',
                flashSent: 'Message sent from template, id={id}'
            }
        }
    };

    function normalizeLang(l) {
        if (!l) return 'en';
        var s = String(l).toLowerCase();
        if (s === 'ru' || s.indexOf('ru') === 0) return 'ru';
        return 'en';
    }

    function readLangFromQuery() {
        try {
            var params = new URLSearchParams(window.location.search);
            return params.get('lang');
        } catch (e) {
            return null;
        }
    }

    function getLang() {
        var q = readLangFromQuery();
        if (q) {
            var n = normalizeLang(q);
            try {
                localStorage.setItem(STORAGE_KEY, n);
            } catch (e) { /* ignore */ }
            return n;
        }
        try {
            var stored = localStorage.getItem(STORAGE_KEY);
            if (stored) return normalizeLang(stored);
        } catch (e) { /* ignore */ }
        return normalizeLang(navigator.language || 'en');
    }

    var currentLang = getLang();

    function setLang(lang) {
        var n = normalizeLang(lang);
        try {
            localStorage.setItem(STORAGE_KEY, n);
        } catch (e) { /* ignore */ }
        currentLang = n;
        window.location.reload();
    }

    function lookup(key) {
        var parts = key.split('.');
        var o = STRINGS[currentLang];
        for (var i = 0; i < parts.length; i++) {
            o = o && o[parts[i]];
        }
        return typeof o === 'string' ? o : null;
    }

    function t(key, vars) {
        var s = lookup(key);
        if (s == null) {
            console.warn('[i18n] missing', key, currentLang);
            return key;
        }
        if (vars) {
            for (var k in vars) {
                if (Object.prototype.hasOwnProperty.call(vars, k)) {
                    var re = new RegExp('\\{' + k + '\\}', 'g');
                    s = s.replace(re, String(vars[k]));
                }
            }
        }
        return s;
    }

    function applyI18n(root) {
        var el = root || document;
        el.querySelectorAll('[data-i18n]').forEach(function (node) {
            var key = node.getAttribute('data-i18n');
            node.textContent = t(key);
        });
        el.querySelectorAll('[data-i18n-html]').forEach(function (node) {
            var key = node.getAttribute('data-i18n-html');
            node.innerHTML = t(key);
        });
        el.querySelectorAll('[data-i18n-placeholder]').forEach(function (node) {
            var key = node.getAttribute('data-i18n-placeholder');
            node.setAttribute('placeholder', t(key));
        });
        el.querySelectorAll('[data-i18n-title]').forEach(function (node) {
            node.setAttribute('title', t(node.getAttribute('data-i18n-title')));
        });
        el.querySelectorAll('[data-i18n-aria-label]').forEach(function (node) {
            var key = node.getAttribute('data-i18n-aria-label');
            node.setAttribute('aria-label', t(key));
        });
        var titleNode = document.querySelector('[data-i18n-document-title]');
        if (titleNode) {
            document.title = t(titleNode.getAttribute('data-i18n-document-title'));
        }
    }

    function mountLangSwitch(container) {
        if (!container) return;
        container.innerHTML = '';
        var wrap = document.createElement('div');
        wrap.className = 'lang-switch';
        wrap.setAttribute('role', 'group');
        wrap.setAttribute('aria-label', t('common.language'));
        ['ru', 'en'].forEach(function (lang) {
            var b = document.createElement('button');
            b.type = 'button';
            b.className = 'lang-btn' + (currentLang === lang ? ' active' : '');
            b.textContent = lang.toUpperCase();
            b.setAttribute('aria-pressed', currentLang === lang ? 'true' : 'false');
            b.addEventListener('click', function () {
                if (currentLang !== lang) setLang(lang);
            });
            wrap.appendChild(b);
        });
        container.appendChild(wrap);
    }

    document.documentElement.lang = currentLang === 'ru' ? 'ru' : 'en';

    window.DusterI18n = {
        t: t,
        applyI18n: applyI18n,
        mountLangSwitch: mountLangSwitch,
        setLang: setLang,
        getLang: function () { return currentLang; },
        getLocaleTag: function () { return currentLang === 'ru' ? 'ru-RU' : 'en-US'; }
    };
})();
