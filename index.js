import { AppRegistry } from 'react-native';
import App from './src/App';

AppRegistry.registerComponent('PulseApp', () => App);
AppRegistry.runApplication('PulseApp', {
  initialProps: {},
  rootTag: document.getElementById('root'),
});
