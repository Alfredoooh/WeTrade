import { AppRegistry } from 'react-native';
import App from './src/App';

AppRegistry.registerComponent('GoalZone', () => App);
AppRegistry.runApplication('GoalZone', {
  initialProps: {},
  rootTag: document.getElementById('root'),
});