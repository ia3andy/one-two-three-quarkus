import {Howl, Howler} from 'howler';

window.audio = {
    game: new Howl({src: ["/static/audio/rock.mp3"], loop: true, html5: true}),
};


