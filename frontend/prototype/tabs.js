(() => {
    const links = [...document.querySelectorAll('.view-tab')];
    if (!links.length) return;
    const panels = [...document.querySelectorAll('.view-panel')];

    function show(id) {
        if (!panels.some((panel) => panel.id === id)) id = panels[0].id;
        panels.forEach((panel) => {
            panel.hidden = panel.id !== id;
        });
        links.forEach((link) => {
            const selected = link.hash === '#' + id;
            link.setAttribute('aria-selected', String(selected));
            link.tabIndex = selected ? 0 : -1;
        });
    }

    document.addEventListener('click', (event) => {
        const link = event.target.closest('a[href^="#"]');
        if (!link || !panels.some((panel) => '#' + panel.id === link.hash))
            return;
        event.preventDefault();
        history.replaceState(null, '', link.hash);
        show(link.hash.slice(1));
    });

    document.addEventListener('keydown', (event) => {
        const current = links.indexOf(document.activeElement);
        if (
            current < 0 ||
            !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)
        )
            return;
        event.preventDefault();
        const next =
            event.key === 'Home'
                ? 0
                : event.key === 'End'
                  ? links.length - 1
                  : (current +
                        (event.key === 'ArrowRight' ? 1 : -1) +
                        links.length) %
                    links.length;
        links[next].click();
        links[next].focus();
    });

    window.addEventListener('hashchange', () => show(location.hash.slice(1)));
    show(location.hash.slice(1));
    window.scrollTo(0, 0);
})();
